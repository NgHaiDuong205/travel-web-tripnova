package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.PlannerDayDTO;
import com.duong.travelweb.model.dto.PlannerRequestDTO;
import com.duong.travelweb.model.dto.PlannerResultDTO;
import com.duong.travelweb.model.dto.PlannerStopDTO;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.repository.DestinationRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.LandmarkRepository;
import com.duong.travelweb.service.ItineraryPlannerService;
import com.duong.travelweb.service.RoutingService;
import com.duong.travelweb.util.GeoPlanning;
import com.duong.travelweb.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Planner — chia việc để không bịa địa điểm:
 * <ol>
 *   <li>Thuật toán (ở đây) chấm điểm địa danh thật của điểm đến theo sở thích, gom thành từng ngày theo toạ độ
 *       ({@link GeoPlanning#balancedClusters}), chọn khách sạn gần trọng tâm hợp ngân sách và các quán ăn gần từng cụm.</li>
 *   <li>AI (dịch vụ Python) chỉ chọn trong các mã ref của đúng ngày đó và viết tiêu đề / chủ đề / ghi chú.
 *       Mọi ref được kiểm tra lại; thiếu / sai thì lấy ứng viên điểm cao nhất. AI lỗi → vẫn trả lịch trình (không chữ AI).</li>
 *   <li>Thứ tự đi trong ngày: nhanh nhất từ khách sạn ({@link GeoPlanning#bestOpenPath}) theo thời gian đi đường thật
 *       ({@link RoutingService}, OSRM); quãng ngắn thì đi bộ. Không lấy được đường đi → ước lượng đường chim bay × hệ số
 *       đường. Thời lượng tham quan theo loại hình, giờ mở cửa nếu đọc được.</li>
 * </ol>
 */
@Service
public class ItineraryPlannerServiceImpl implements ItineraryPlannerService {
    private static final Logger log = LoggerFactory.getLogger(ItineraryPlannerServiceImpl.class);

    private static final String RESTAURANT = "restaurant";
    private static final Map<String, Set<String>> INTEREST_CATEGORIES = Map.of(
            "culture", Set.of("historical", "museum", "temple"),
            "nature", Set.of("park", "mountain"),
            "beach", Set.of("beach"),
            "food", Set.of("market"),
            "shopping", Set.of("market"),
            "entertainment", Set.of("entertainment", "sport"),
            "wellness", Set.of("wellness"));
    /** Thời lượng tham quan (phút) theo loại hình. */
    private static final Map<String, Integer> VISIT_MINUTES = Map.ofEntries(
            Map.entry("museum", 90), Map.entry("historical", 75), Map.entry("temple", 60), Map.entry("park", 75),
            Map.entry("beach", 150), Map.entry("mountain", 180), Map.entry("market", 75),
            Map.entry("entertainment", 120), Map.entry("wellness", 120), Map.entry("sport", 90));
    /** Chi phí ăn ước tính mỗi người mỗi bữa (USD) theo mức ngân sách. */
    private static final Map<String, BigDecimal> MEAL_COST = Map.of(
            "budget", new BigDecimal("5"), "mid", new BigDecimal("12"), "luxury", new BigDecimal("30"));
    /** Tên (đã bỏ dấu) của nơi buôn bán — dữ liệu xếp chung vào "market" với chợ truyền thống. */
    private static final Pattern COMMERCIAL = Pattern.compile(
            "sieu thi|supermarket|\\bmart\\b|\\bmall\\b|trung tam thuong mai|vincom|aeon|big c|coopmart|plaza"
                    + "|cong ty|cua hang|\\bstore\\b|\\bshop\\b|bakery|^co so");
    private static final Pattern TRADITIONAL_MARKET = Pattern.compile("^cho |\\bcho dem\\b|night market");
    /** Chỉ sôi động buổi tối → xếp sau bữa tối. */
    private static final Pattern NIGHT = Pattern.compile("\\bcho dem\\b|night market|pho di bo|walking street");
    /** Khu du lịch lớn / công viên chủ đề: cần nửa ngày. */
    private static final Pattern THEME_PARK = Pattern.compile("khu du lich|sun ?world|vinwonders|vinpearl|ba na hills");
    private static final Pattern HOURS = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*[-–]\\s*(\\d{1,2}):(\\d{2})");
    private static final int LATEST_MINUTE = 23 * 60 + 30;
    private static final double MEAL_RADIUS_KM = 5;
    private static final int MEAL_CANDIDATES = 6;
    private static final int MAX_SIGHT_CANDIDATES_PER_DAY = 12;
    /** Quãng đường bộ ≤ ngưỡng này thì đi bộ (4,5 km/h). */
    private static final double WALK_MAX_KM = 1.0;
    private static final double WALK_KMH = 4.5;
    /** Thời gian OSRM là đường thông thoáng → nhân hệ số kẹt xe nội đô + phút gọi xe / gửi xe. */
    private static final double TRAFFIC_FACTOR = 1.3;
    private static final int DRIVE_OVERHEAD_MINUTES = 6;

    private final DestinationRepository destinationRepository;
    private final LandmarkRepository landmarkRepository;
    private final HotelRepository hotelRepository;
    private final AiServiceClient aiServiceClient;
    private final RoutingService routingService;
    private final int rateLimitMax;
    private final int rateLimitWindowMinutes;
    /** Giới hạn tần suất trong bộ nhớ (một instance BE): mỗi lần tạo tốn 1 lượt gọi LLM. */
    private final Map<UUID, Deque<Long>> recentPlans = new ConcurrentHashMap<>();

    public ItineraryPlannerServiceImpl(DestinationRepository destinationRepository,
                                       LandmarkRepository landmarkRepository,
                                       HotelRepository hotelRepository,
                                       AiServiceClient aiServiceClient,
                                       RoutingService routingService,
                                       @Value("${app.ai.planner.rate-limit.max:10}") int rateLimitMax,
                                       @Value("${app.ai.planner.rate-limit.window-minutes:10}") int rateLimitWindowMinutes) {
        this.destinationRepository = destinationRepository;
        this.landmarkRepository = landmarkRepository;
        this.hotelRepository = hotelRepository;
        this.aiServiceClient = aiServiceClient;
        this.routingService = routingService;
        this.rateLimitMax = rateLimitMax;
        this.rateLimitWindowMinutes = rateLimitWindowMinutes;
    }

    /** Địa danh / quán ăn ứng viên. samePoint = số địa danh trùng toạ độ (≥ 3 → toạ độ cấp xã, kém tin cậy). */
    private record Place(UUID id, String name, String category, String description, String cover, String address,
                         String openingHours, BigDecimal entryFee, double[] point, Double avgRating, int reviewCount,
                         int samePoint) {
    }

    private record HotelOption(UUID id, String name, Integer stars, String cover, String address, double[] point,
                               BigDecimal price, Double avgRating, int reviewCount, int samePoint) {
    }

    /** Ứng viên của một ngày (trước khi chọn). */
    private static final class DayDraft {
        final List<Place> sights = new ArrayList<>();
        final List<Place> meals = new ArrayList<>();
        int need;
    }

    /** Lựa chọn cuối của một ngày (sau AI / dự phòng). */
    private record DayChoice(List<Place> sights, Place lunch, Place dinner, String theme, Map<UUID, String> notes) {
    }

    /** Một chặng: km đường bộ (null khi thiếu toạ độ), phút (đã làm tròn 5), mode walk|drive (null khi thiếu toạ độ). */
    private record Leg(Double km, int minutes, String mode) {
    }

    /**
     * Di chuyển giữa các điểm của một ngày: ma trận OSRM (km + giây, theo khoá toạ độ) nếu lấy được, không thì ước lượng
     * đường chim bay × 1.35 (22 km/h nội đô, 40 km/h nếu > 10 km, + 8 phút) như trước.
     */
    private static final class DayTravel {
        private final Map<String, Integer> index;
        private final RoutingService.Matrix matrix;

        DayTravel(Map<String, Integer> index, RoutingService.Matrix matrix) {
            this.index = index;
            this.matrix = matrix;
        }

        static DayTravel estimate() {
            return new DayTravel(Map.of(), null);
        }

        boolean routed() {
            return matrix != null;
        }

        /** Phút chưa làm tròn — chi phí để sắp thứ tự điểm. */
        double cost(double[] from, double[] to) {
            if (from == null || to == null) {
                return 15;
            }
            double[] road = road(from, to);
            return road[0] <= WALK_MAX_KM ? road[0] / WALK_KMH * 60 : road[1];
        }

        Leg leg(double[] from, double[] to) {
            if (from == null || to == null) {
                return new Leg(null, 15, null);
            }
            double[] road = road(from, to);
            boolean walk = road[0] <= WALK_MAX_KM;
            double minutes = walk ? road[0] / WALK_KMH * 60 + 2 : road[1];
            return new Leg(round1(road[0]), Math.max(5, (int) Math.ceil(minutes / 5) * 5), walk ? "walk" : "drive");
        }

        /** {km đường bộ, phút đi xe (đã cộng kẹt xe + gọi xe)}. */
        private double[] road(double[] from, double[] to) {
            Integer i = index.get(key(from));
            Integer j = index.get(key(to));
            if (matrix != null && i != null && j != null) {
                double km = matrix.km()[i][j];
                double seconds = matrix.seconds()[i][j];
                if (Double.isFinite(km) && Double.isFinite(seconds)) {
                    return new double[]{km, seconds / 60 * TRAFFIC_FACTOR + DRIVE_OVERHEAD_MINUTES};
                }
            }
            double km = GeoPlanning.haversineKm(from, to) * 1.35;
            return new double[]{km, km / (km < 10 ? 22 : 40) * 60 + 8};
        }

        static String key(double[] p) {
            return String.format(Locale.ROOT, "%.5f,%.5f", p[0], p[1]);
        }
    }

    /** radiusKm: phạm vi di chuyển tối đa tính từ nơi lưu trú, null = không giới hạn. */
    private record Options(String locale, String budget, String pace, List<String> interests, int days, int partySize,
                           Random random, boolean jitter, Integer radiusKm) {
        boolean vi() {
            return "vi".equals(locale);
        }
    }

    @Override
    public PlannerResultDTO plan(UUID userId, PlannerRequestDTO request) {
        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today)) {
            throw ApiException.badRequest("Ngày bắt đầu không được ở trong quá khứ");
        }
        if (request.getStartDate().isAfter(today.plusYears(1))) {
            throw ApiException.badRequest("Chỉ lên kế hoạch trong vòng 1 năm tới");
        }
        DestinationEntity destination = destinationRepository.findById(request.getDestinationId())
                .filter(d -> !Boolean.FALSE.equals(d.getIsActive()))
                .orElseThrow(() -> ApiException.badRequest("Điểm đến không tồn tại"));
        checkRateLimit(userId);

        int variant = request.getVariant() == null ? 0 : request.getVariant();
        Options opt = new Options(
                "en".equals(request.getLocale()) ? "en" : "vi",
                blankOr(request.getBudget(), "mid"),
                blankOr(request.getPace(), "balanced"),
                new ArrayList<>(new LinkedHashSet<>(request.getInterests() == null ? List.of() : request.getInterests())),
                request.getDays(), request.getPartySize(),
                new Random(destination.getId().getLeastSignificantBits() * 31 + variant),
                variant > 0, request.getRadiusKm());
        List<String> warnings = new ArrayList<>();

        // ---- 1. Dữ liệu + loại toạ độ lệch xa (geocode sai)
        List<Place> all = loadPlaces(destination.getId());
        double[] center = destination.getLatitude() != null && destination.getLongitude() != null
                ? new double[]{destination.getLatitude(), destination.getLongitude()} : null;
        center = dropOutliers(all, center);
        List<Place> sights = new ArrayList<>();
        List<Place> restaurants = new ArrayList<>();
        for (Place p : all) {
            (RESTAURANT.equals(p.category()) ? restaurants : sights).add(p);
        }
        Map<UUID, Double> sightScore = new HashMap<>();
        for (Place p : sights) {
            sightScore.put(p.id(), scoreSight(p, opt, center));
        }
        Map<UUID, Double> mealScore = new HashMap<>();
        for (Place p : restaurants) {
            mealScore.put(p.id(), scorePlace(p, opt, 0.8));
        }
        sights.sort(Comparator.comparingDouble((Place p) -> -sightScore.get(p.id())));
        boolean hasRestaurants = !restaurants.isEmpty();

        int perDay = switch (opt.pace()) {
            case "relaxed" -> 2;
            case "packed" -> 4;
            default -> 3;
        };
        int nights = opt.days() - 1;
        int rooms = Math.max(1, (opt.partySize() + 1) / 2);
        int occupancy = Math.min(opt.partySize(), 2);
        List<HotelOption> hotels = new ArrayList<>();
        double[] base;
        List<DayDraft> drafts;

        boolean ranged = opt.radiusKm() != null && sights.stream().anyMatch(p -> p.point() != null);
        if (ranged) {
            // ---- 2a. Có phạm vi di chuyển: chọn khu vực đáng ở nhất → khách sạn trong khu đó → chỉ giữ điểm / quán
            // trong bán kính tính từ khách sạn (thiếu quán thì ăn lại quán cũ, không đi xa hơn).
            double radius = opt.radiusKm();
            double[] anchor = chooseAnchor(sights, sightScore, radius, opt.days() * perDay * 2, center);
            if (nights > 0) {
                hotels = rankHotels(destination.getId(), occupancy, anchor, opt, radius);
                if (hotels.isEmpty()) {
                    hotels = rankHotels(destination.getId(), occupancy, anchor, opt, null);
                    warnings.add(hotels.isEmpty() ? "no_hotel" : "hotel_outside_radius");
                }
            }
            HotelOption chosen = hotels.isEmpty() ? null : hotels.get(0);
            base = chosen != null && chosen.point() != null
                    && GeoPlanning.haversineKm(anchor, chosen.point()) <= radius ? chosen.point() : anchor;
            final double[] from = base;
            int before = sights.size();
            sights.removeIf(p -> p.point() == null || GeoPlanning.haversineKm(from, p.point()) > radius);
            restaurants.removeIf(p -> p.point() == null || GeoPlanning.haversineKm(from, p.point()) > radius);
            for (Place p : sights) {
                sightScore.put(p.id(), scoreSight(p, opt, from)); // trừ điểm theo khoảng cách tới nơi ở
            }
            sights.sort(Comparator.comparingDouble((Place p) -> -sightScore.get(p.id())));
            if (sights.size() < before && sights.size() < opt.days() * perDay) {
                warnings.add("radius_limited");
            }
            drafts = splitIntoDays(sights, opt, perDay, warnings);
        } else {
            // ---- 2b. Không giới hạn: chia ngày trước, khách sạn gần trọng tâm các điểm đã chọn.
            drafts = splitIntoDays(sights, opt, perDay, warnings);
            List<double[]> poolPoints = new ArrayList<>();
            for (DayDraft d : drafts) {
                for (Place p : d.sights) {
                    if (p.point() != null) {
                        poolPoints.add(p.point());
                    }
                }
            }
            double[] target = poolPoints.isEmpty() ? center : GeoPlanning.centroid(poolPoints);
            if (nights > 0) {
                hotels = rankHotels(destination.getId(), occupancy, target, opt, null);
                if (hotels.isEmpty()) {
                    warnings.add("no_hotel");
                }
            }
            HotelOption chosen = hotels.isEmpty() ? null : hotels.get(0);
            base = chosen != null && chosen.point() != null ? chosen.point() : target;
        }
        HotelOption hotel = hotels.isEmpty() ? null : hotels.get(0);

        // Thứ tự ngày: đi lần lượt các khu vực theo đường ngắn nhất từ khách sạn.
        orderDays(drafts, base);

        // ---- 4. Quán ăn gần từng cụm
        for (DayDraft d : drafts) {
            d.meals.addAll(nearbyMeals(d, base, restaurants, mealScore));
        }
        if (!hasRestaurants) {
            warnings.add("no_restaurants");
        } else if (restaurants.isEmpty()) {
            warnings.add("no_restaurants_in_radius"); // có quán nhưng đều ngoài phạm vi → ăn tự do, không đi xa
        }

        // ---- 5. AI chọn + viết (lỗi thì dùng lựa chọn mặc định)
        Map<UUID, String> refs = new HashMap<>();
        Map<String, Place> byRef = new HashMap<>();
        JsonNode ai = callAi(destination, opt, request.getNote(), hotel, drafts, refs, byRef, warnings);
        JsonNode aiPlan = ai == null ? null : ai.get("plan");

        Map<Integer, JsonNode> aiDays = new HashMap<>();
        if (aiPlan != null && aiPlan.get("days") != null && aiPlan.get("days").isArray()) {
            for (JsonNode day : aiPlan.get("days")) {
                if (day.hasNonNull("day")) {
                    aiDays.putIfAbsent(day.get("day").asInt(), day);
                }
            }
        }
        Set<UUID> used = new HashSet<>();
        List<DayChoice> choices = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            choices.add(choose(drafts.get(i), aiDays.get(i + 1), byRef, used, sightScore, opt, i + 1, warnings));
        }

        // ---- 6. Đường đi thật (song song từng ngày) → xếp giờ + chi phí
        List<DayTravel> travels = dayTravels(choices, base, warnings);
        PlannerResultDTO result = new PlannerResultDTO();
        PlannerStopDTO hotelStop = hotel == null ? null : hotelStop(hotel, nights, rooms, opt);
        if (hotelStop != null && aiPlan != null) {
            hotelStop.setNotes(text(aiPlan, "hotelNote"));
        }
        BigDecimal total = hotelStop == null || hotelStop.getEstimatedCost() == null
                ? BigDecimal.ZERO : hotelStop.getEstimatedCost();
        for (int i = 0; i < choices.size(); i++) {
            PlannerDayDTO day = schedule(i + 1, request.getStartDate().plusDays(i), choices.get(i), hotelStop, base,
                    travels.get(i), opt);
            for (PlannerStopDTO stop : day.getItems()) {
                if (stop.getEstimatedCost() != null && !"hotel".equals(stop.getKind())) {
                    total = total.add(stop.getEstimatedCost());
                }
            }
            result.getDayPlans().add(day);
        }

        String destinationName = destination.getName();
        String fallbackTitle = opt.vi() ? destinationName + " " + opt.days() + " ngày"
                : opt.days() + " days in " + destinationName;
        result.setTitle(aiPlan != null && text(aiPlan, "title") != null ? text(aiPlan, "title") : fallbackTitle);
        result.setSummary(aiPlan == null ? null : text(aiPlan, "summary"));
        result.setHotelNote(hotelStop == null ? null : hotelStop.getNotes());
        if (aiPlan != null && aiPlan.get("tips") != null && aiPlan.get("tips").isArray()) {
            for (JsonNode tip : aiPlan.get("tips")) {
                if (tip.isString() && !tip.asString().isBlank()) {
                    result.getTips().add(tip.asString());
                }
            }
        }
        result.setAiUsed(aiPlan != null);
        result.setModel(ai == null ? null : text(ai, "model"));
        result.setWarnings(new ArrayList<>(new LinkedHashSet<>(warnings)));
        result.setDestinationId(destination.getId());
        result.setDestinationName(destinationName);
        result.setCountryName(destination.getCountry() == null ? null : destination.getCountry().getName());
        if (base != null) {
            result.setCenterLatitude(base[0]);
            result.setCenterLongitude(base[1]);
        }
        result.setStartDate(request.getStartDate());
        result.setEndDate(request.getStartDate().plusDays(opt.days() - 1L));
        result.setDays(opt.days());
        result.setPartySize(opt.partySize());
        result.setBudget(opt.budget());
        result.setPace(opt.pace());
        result.setInterests(opt.interests());
        result.setHotel(hotelStop);
        for (int i = 1; i < hotels.size(); i++) {
            result.getHotelAlternatives().add(hotelStop(hotels.get(i), nights, rooms, opt));
        }
        result.setEstimatedTotal(total.setScale(2, RoundingMode.HALF_UP));
        result.setPrompt(promptText(opt, request.getNote()));
        result.setRadiusKm(ranged ? opt.radiusKm() : null);
        return result;
    }

    // ================================================================== dữ liệu

    private List<Place> loadPlaces(UUID destinationId) {
        List<Place> places = new ArrayList<>();
        for (Object[] r : landmarkRepository.findPlannerCandidates(destinationId)) {
            double[] point = r[8] != null && r[9] != null
                    ? new double[]{((Number) r[8]).doubleValue(), ((Number) r[9]).doubleValue()} : null;
            places.add(new Place((UUID) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4],
                    (String) r[5], (String) r[6], toDecimal(r[7]), point,
                    r[10] == null ? null : ((Number) r[10]).doubleValue(), ((Number) r[11]).intValue(),
                    ((Number) r[12]).intValue()));
        }
        return places;
    }

    /**
     * Toạ độ cách tâm quá xa so với phần lớn dữ liệu (geocode nhầm tỉnh khác) → coi như không có toạ độ.
     * Ngưỡng = 2 × khoảng cách phân vị 75, trong [20, 100] km. Trả tâm dùng tiếp (tâm điểm đến hoặc trung vị dữ liệu).
     */
    private double[] dropOutliers(List<Place> places, double[] center) {
        List<double[]> points = new ArrayList<>();
        for (Place p : places) {
            if (p.point() != null) {
                points.add(p.point());
            }
        }
        if (points.isEmpty()) {
            return center;
        }
        if (center == null) {
            List<Double> lats = new ArrayList<>();
            List<Double> lngs = new ArrayList<>();
            for (double[] p : points) {
                lats.add(p[0]);
                lngs.add(p[1]);
            }
            lats.sort(null);
            lngs.sort(null);
            center = new double[]{lats.get(lats.size() / 2), lngs.get(lngs.size() / 2)};
        }
        List<Double> distances = new ArrayList<>();
        for (double[] p : points) {
            distances.add(GeoPlanning.haversineKm(center, p));
        }
        distances.sort(null);
        double p75 = distances.get((int) Math.floor(0.75 * (distances.size() - 1)));
        double limit = Math.min(100, Math.max(20, 2 * p75));
        for (int i = 0; i < places.size(); i++) {
            Place p = places.get(i);
            if (p.point() != null && GeoPlanning.haversineKm(center, p.point()) > limit) {
                places.set(i, new Place(p.id(), p.name(), p.category(), p.description(), p.cover(), p.address(),
                        p.openingHours(), p.entryFee(), null, p.avgRating(), p.reviewCount(), p.samePoint()));
            }
        }
        return center;
    }

    // ================================================================== chấm điểm

    private double scoreSight(Place p, Options opt, double[] center) {
        double score = scorePlace(p, opt, 1.2);
        boolean matches = false;
        for (String interest : opt.interests()) {
            if (INTEREST_CATEGORIES.getOrDefault(interest, Set.of()).contains(p.category())) {
                matches = true;
            }
        }
        String name = StringUtil.removeAccents(p.name());
        boolean shopping = opt.interests().contains("shopping");
        if (COMMERCIAL.matcher(name).find()) {
            // Siêu thị / TTTM / cửa hàng: chỉ đáng đi khi khách thích mua sắm, và vẫn kém chợ truyền thống.
            score -= shopping ? 0.8 : 3;
        } else if (TRADITIONAL_MARKET.matcher(name).find()) {
            score += 0.5;
            if (opt.interests().contains("food") || opt.interests().contains("culture")) {
                matches = true;
            }
        }
        // Xa trung tâm thì trừ dần (giữ mỗi ngày gọn), trừ nhẹ hơn cho thiên nhiên / biển khách đã chọn.
        if (center != null && p.point() != null) {
            double km = GeoPlanning.haversineKm(center, p.point());
            double penalty = Math.min(1.5, Math.max(0, km - 6) * 0.08);
            score -= matches && ("mountain".equals(p.category()) || "beach".equals(p.category())
                    || "park".equals(p.category())) ? penalty / 2 : penalty;
        }
        if (matches) {
            score += 2;
        } else if (!opt.interests().isEmpty()) {
            score -= 0.3;
        }
        if ("other".equals(p.category())) {
            score -= 0.4;
        }
        // Spa / sân thể thao ít là "điểm tham quan" trừ khi khách thích.
        if (!matches && ("wellness".equals(p.category()) || "sport".equals(p.category()))) {
            score -= 0.8;
        }
        return score;
    }

    /** Điểm chung: có ảnh, có mô tả, đánh giá, toạ độ đáng tin; jitter khi khách bấm "tạo lại" (variant > 0). */
    private double scorePlace(Place p, Options opt, double jitterScale) {
        double score = 1;
        if (p.cover() != null && !p.cover().isBlank()) {
            score += 0.6;
        }
        if (p.description() != null) {
            score += Math.min(p.description().length() / 400.0, 1) * 0.5;
        }
        if (p.reviewCount() > 0 && p.avgRating() != null) {
            score += (p.avgRating() - 3) * 0.4 + Math.min(p.reviewCount(), 10) * 0.05;
        }
        if (p.point() == null) {
            score -= 0.5;
        } else if (p.samePoint() >= 3) {
            score -= 0.7;
        }
        if (opt.jitter()) {
            score += opt.random().nextDouble() * jitterScale;
        }
        return score;
    }

    // ================================================================== chia ngày

    private List<DayDraft> splitIntoDays(List<Place> sights, Options opt, int perDay, List<String> warnings) {
        int days = opt.days();
        List<DayDraft> drafts = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            drafts.add(new DayDraft());
        }
        List<Place> located = new ArrayList<>();
        for (Place p : sights) {
            if (p.point() != null) {
                located.add(p);
            }
        }
        int needed = days * perDay;
        if (located.size() >= days) {
            // Mỗi ngày ~1.8 lần số cần để AI có lựa chọn; danh sách đã xếp theo điểm.
            int poolSize = Math.min(located.size(), Math.min(days * MAX_SIGHT_CANDIDATES_PER_DAY,
                    Math.max(needed + days, (int) Math.round(needed * 1.8))));
            List<Place> pool = new ArrayList<>(located.subList(0, poolSize));
            List<double[]> points = new ArrayList<>();
            for (Place p : pool) {
                points.add(p.point());
            }
            int[] labels = GeoPlanning.balancedClusters(points, days, opt.random().nextLong());
            for (int i = 0; i < pool.size(); i++) {
                drafts.get(labels[i]).sights.add(pool.get(i)); // giữ thứ tự điểm giảm dần trong từng ngày
            }
            if (located.size() < needed) {
                warnings.add("few_places");
            }
        } else {
            // Quá ít điểm có toạ độ: chia vòng theo điểm số (kể cả điểm không toạ độ), bản đồ sẽ thiếu ghim.
            warnings.add(located.isEmpty() ? "no_coordinates" : "few_places");
            List<Place> pool = sights.subList(0, Math.min(sights.size(), (int) Math.round(needed * 1.5)));
            for (int i = 0; i < pool.size(); i++) {
                drafts.get(i % days).sights.add(pool.get(i));
            }
            if (sights.size() < needed) {
                warnings.add("few_places");
            }
        }
        for (DayDraft d : drafts) {
            d.need = Math.min(perDay, d.sights.size());
        }
        return drafts;
    }

    /** Sắp các ngày theo đường ngắn nhất qua trọng tâm từng cụm, xuất phát từ khách sạn. */
    private void orderDays(List<DayDraft> drafts, double[] base) {
        List<DayDraft> withCenter = new ArrayList<>();
        List<double[]> centers = new ArrayList<>();
        List<DayDraft> rest = new ArrayList<>();
        for (DayDraft d : drafts) {
            double[] c = draftCenter(d);
            if (c == null) {
                rest.add(d);
            } else {
                withCenter.add(d);
                centers.add(c);
            }
        }
        if (withCenter.size() < 2) {
            return;
        }
        int[] order = GeoPlanning.bestOpenPath(base, centers);
        List<DayDraft> sorted = new ArrayList<>();
        for (int i : order) {
            sorted.add(withCenter.get(i));
        }
        sorted.addAll(rest);
        drafts.clear();
        drafts.addAll(sorted);
    }

    private double[] draftCenter(DayDraft d) {
        List<double[]> points = new ArrayList<>();
        for (Place p : d.sights) {
            if (p.point() != null) {
                points.add(p.point());
            }
        }
        return points.isEmpty() ? null : GeoPlanning.centroid(points);
    }

    // ================================================================== khách sạn & quán ăn

    /**
     * Tâm khu vực khi khách giới hạn phạm vi: thử tâm điểm đến và vị trí 40 điểm tham quan điểm cao nhất, chọn nơi mà
     * tổng điểm của `take` điểm hay nhất trong bán kính là lớn nhất (bằng nhau thì gần tâm điểm đến hơn).
     */
    private double[] chooseAnchor(List<Place> sights, Map<UUID, Double> score, double radius, int take, double[] center) {
        List<double[]> candidates = new ArrayList<>();
        if (center != null) {
            candidates.add(center);
        }
        for (Place p : sights) {
            if (p.point() != null && candidates.size() < 41) {
                candidates.add(p.point());
            }
        }
        double[] best = candidates.get(0);
        double bestValue = -1;
        for (double[] c : candidates) {
            List<Double> inside = new ArrayList<>();
            for (Place p : sights) {
                if (p.point() != null && GeoPlanning.haversineKm(c, p.point()) <= radius) {
                    inside.add(Math.max(0.1, score.get(p.id())));
                }
            }
            inside.sort(Comparator.reverseOrder());
            double value = 0;
            for (int i = 0; i < Math.min(take, inside.size()); i++) {
                value += inside.get(i);
            }
            if (value > bestValue + 1e-9
                    || (Math.abs(value - bestValue) <= 1e-9 && center != null
                    && GeoPlanning.haversineKm(center, c) < GeoPlanning.haversineKm(center, best))) {
                bestValue = value;
                best = c;
            }
        }
        return best;
    }

    /** radiusKm khác null: chỉ khách sạn có toạ độ trong bán kính quanh target. */
    private List<HotelOption> rankHotels(UUID destinationId, int occupancy, double[] target, Options opt, Double radiusKm) {
        double[] priceRange = switch (opt.budget()) {
            case "budget" -> new double[]{0, 45};
            case "luxury" -> new double[]{100, Double.MAX_VALUE};
            default -> new double[]{35, 130};
        };
        int[] starRange = switch (opt.budget()) {
            case "budget" -> new int[]{1, 3};
            case "luxury" -> new int[]{4, 5};
            default -> new int[]{3, 4};
        };
        Map<HotelOption, Double> scores = new HashMap<>();
        for (Object[] r : hotelRepository.findPlannerCandidates(destinationId, occupancy)) {
            double[] point = r[5] != null && r[6] != null
                    ? new double[]{((Number) r[5]).doubleValue(), ((Number) r[6]).doubleValue()} : null;
            HotelOption h = new HotelOption((UUID) r[0], (String) r[1], r[2] == null ? null : ((Number) r[2]).intValue(),
                    (String) r[3], (String) r[4], point, toDecimal(r[7]),
                    r[8] == null ? null : ((Number) r[8]).doubleValue(), ((Number) r[9]).intValue(),
                    ((Number) r[10]).intValue());
            if (radiusKm != null && (point == null || target == null
                    || GeoPlanning.haversineKm(target, point) > radiusKm)) {
                continue;
            }
            double score = 0;
            double price = h.price().doubleValue();
            if (price >= priceRange[0] && price <= priceRange[1]) {
                score += 2;
            } else {
                double deviation = price < priceRange[0] ? (priceRange[0] - price) / priceRange[0]
                        : (price - priceRange[1]) / priceRange[1];
                score -= Math.min(2, deviation * 2);
            }
            if (h.stars() != null && h.stars() >= starRange[0] && h.stars() <= starRange[1]) {
                score += 1;
            }
            if (target != null) {
                score -= point == null ? 3 : 0.3 * Math.min(GeoPlanning.haversineKm(target, point), 15);
            }
            if (h.reviewCount() > 0 && h.avgRating() != null) {
                score += (h.avgRating() - 3) * 0.6 + Math.min(h.reviewCount(), 20) * 0.03;
            }
            if (h.cover() != null && !h.cover().isBlank()) {
                score += 0.5;
            }
            if (h.samePoint() >= 3) {
                score -= 0.7;
            }
            if (opt.jitter()) {
                score += opt.random().nextDouble() * 1.2;
            }
            scores.put(h, score);
        }
        List<HotelOption> ranked = new ArrayList<>(scores.keySet());
        ranked.sort(Comparator.comparingDouble((HotelOption h) -> -scores.get(h)).thenComparing(h -> h.id().toString()));
        return new ArrayList<>(ranked.subList(0, Math.min(4, ranked.size())));
    }

    /** Quán ăn gần trọng tâm cụm (≤ 5 km, thiếu thì nới 10 km); không có toạ độ thì lấy quán điểm cao. */
    private List<Place> nearbyMeals(DayDraft d, double[] base, List<Place> restaurants, Map<UUID, Double> mealScore) {
        double[] anchor = draftCenter(d);
        if (anchor == null) {
            anchor = base;
        }
        List<Place> result = new ArrayList<>();
        if (anchor != null) {
            final double[] a = anchor;
            for (double radius : new double[]{MEAL_RADIUS_KM, MEAL_RADIUS_KM * 2}) {
                result.clear();
                for (Place p : restaurants) {
                    if (p.point() != null && GeoPlanning.haversineKm(a, p.point()) <= radius) {
                        result.add(p);
                    }
                }
                if (result.size() >= 2) {
                    break;
                }
            }
            result.sort(Comparator.comparingDouble(
                    (Place p) -> -(mealScore.get(p.id()) - 0.35 * GeoPlanning.haversineKm(a, p.point()))));
        }
        if (result.isEmpty()) {
            result.addAll(restaurants);
            result.sort(Comparator.comparingDouble((Place p) -> -mealScore.get(p.id())));
        }
        return new ArrayList<>(result.subList(0, Math.min(MEAL_CANDIDATES, result.size())));
    }

    // ================================================================== AI

    private JsonNode callAi(DestinationEntity destination, Options opt, String note, HotelOption hotel,
                            List<DayDraft> drafts, Map<UUID, String> refs, Map<String, Place> byRef,
                            List<String> warnings) {
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            DayDraft d = drafts.get(i);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("day", i + 1);
            day.put("need", d.need);
            day.put("sights", candidates(d.sights, "s", refs, byRef));
            day.put("meals", candidates(d.meals, "m", refs, byRef));
            days.add(day);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locale", opt.locale());
        body.put("destination", destination.getName());
        if (destination.getCountry() != null) {
            body.put("country", destination.getCountry().getName());
        }
        body.put("partySize", opt.partySize());
        body.put("budget", opt.budget());
        body.put("pace", opt.pace());
        body.put("interests", opt.interests());
        if (note != null && !note.isBlank()) {
            body.put("note", note.trim());
        }
        if (hotel != null) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("name", hotel.name());
            h.put("stars", hotel.stars());
            body.put("hotel", h);
        }
        body.put("days", days);
        if (!aiServiceClient.isConfigured()) {
            warnings.add("ai_unavailable");
            return null;
        }
        try {
            return aiServiceClient.writeItinerary(body);
        } catch (AiServiceClient.AiException ex) {
            log.warn("AI planner unavailable ({}): {}", ex.getCode(), ex.getMessage());
            warnings.add(AiServiceClient.ERR_QUOTA.equals(ex.getCode()) ? "ai_quota" : "ai_unavailable");
        } catch (RuntimeException ex) {
            log.warn("AI planner failed", ex);
            warnings.add("ai_unavailable");
        }
        return null;
    }

    private List<Map<String, Object>> candidates(List<Place> places, String prefix, Map<UUID, String> refs,
                                                 Map<String, Place> byRef) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Place p : places) {
            String ref = refs.computeIfAbsent(p.id(), id -> prefix + (refs.size() + 1));
            byRef.put(ref, p);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ref", ref);
            item.put("name", truncate(p.name(), 300));
            if (p.category() != null) {
                item.put("category", p.category());
            }
            if (p.description() != null && !p.description().isBlank()) {
                item.put("description", truncate(p.description(), 240));
            }
            list.add(item);
        }
        return list;
    }

    /** Lấy lựa chọn của AI cho ngày nếu hợp lệ (ref thuộc đúng ngày, chưa dùng), thiếu thì bù ứng viên điểm cao. */
    private DayChoice choose(DayDraft d, JsonNode aiDay, Map<String, Place> byRef, Set<UUID> used,
                             Map<UUID, Double> sightScore, Options opt, int dayNumber, List<String> warnings) {
        Set<UUID> daySights = new HashSet<>();
        for (Place p : d.sights) {
            daySights.add(p.id());
        }
        Set<UUID> dayMeals = new HashSet<>();
        for (Place p : d.meals) {
            dayMeals.add(p.id());
        }
        List<Place> chosen = new ArrayList<>();
        if (aiDay != null && aiDay.get("sights") != null && aiDay.get("sights").isArray()) {
            for (JsonNode ref : aiDay.get("sights")) {
                Place p = ref.isString() ? byRef.get(ref.asString()) : null;
                if (chosen.size() < d.need && p != null && daySights.contains(p.id()) && !used.contains(p.id())) {
                    chosen.add(p);
                    used.add(p.id());
                }
            }
        }
        List<Place> byScore = new ArrayList<>(d.sights);
        byScore.sort(Comparator.comparingDouble((Place p) -> -sightScore.get(p.id())));
        for (Place p : byScore) {
            if (chosen.size() >= d.need) {
                break;
            }
            if (!used.contains(p.id())) {
                chosen.add(p);
                used.add(p.id());
            }
        }
        Place lunch = pickMeal(aiDay, "lunch", d, dayMeals, byRef, used, null, warnings);
        Place dinner = pickMeal(aiDay, "dinner", d, dayMeals, byRef, used, lunch, warnings);

        Map<UUID, String> notes = new HashMap<>();
        String theme = null;
        if (aiDay != null) {
            theme = text(aiDay, "theme");
            JsonNode aiNotes = aiDay.get("notes");
            if (aiNotes != null && aiNotes.isObject()) {
                for (Map.Entry<String, JsonNode> e : aiNotes.properties()) {
                    Place p = byRef.get(e.getKey());
                    if (p != null && e.getValue().isString()) {
                        notes.put(p.id(), truncate(e.getValue().asString(), 500));
                    }
                }
            }
        }
        if (theme == null && !chosen.isEmpty()) {
            theme = opt.vi() ? "Khám phá quanh " + chosen.get(0).name() : "Around " + chosen.get(0).name();
        } else if (theme == null) {
            theme = opt.vi() ? "Ngày " + dayNumber + ": tự do khám phá" : "Day " + dayNumber + ": free exploring";
        }
        return new DayChoice(chosen, lunch, dinner, truncate(theme, 120), notes);
    }

    /**
     * Quán của AI (đúng ngày, chưa dùng) → quán gần chưa dùng → hết quán mới trong phạm vi thì ĂN LẠI quán đã dùng
     * (khác bữa còn lại trong ngày nếu được) thay vì đi xa hơn; không có quán nào → null (ăn tự do).
     */
    private Place pickMeal(JsonNode aiDay, String field, DayDraft d, Set<UUID> dayMeals, Map<String, Place> byRef,
                           Set<UUID> used, Place sameDayMeal, List<String> warnings) {
        if (aiDay != null && aiDay.hasNonNull(field) && aiDay.get(field).isString()) {
            Place p = byRef.get(aiDay.get(field).asString());
            if (p != null && dayMeals.contains(p.id()) && !used.contains(p.id())) {
                used.add(p.id());
                return p;
            }
        }
        for (Place p : d.meals) {
            if (!used.contains(p.id())) {
                used.add(p.id());
                return p;
            }
        }
        Place repeat = null;
        for (Place p : d.meals) {
            if (repeat == null || (sameDayMeal != null && repeat.id().equals(sameDayMeal.id()))) {
                repeat = p;
            }
        }
        if (repeat != null) {
            warnings.add("meals_repeated");
        }
        return repeat;
    }

    // ================================================================== xếp giờ

    /**
     * Ma trận đường đi cho từng ngày (khách sạn + điểm tham quan + 2 quán), gọi song song. Ngày nào có ≥ 2 điểm mà không
     * lấy được (dịch vụ tắt / lỗi) → ước lượng + cảnh báo routing_estimated.
     */
    private List<DayTravel> dayTravels(List<DayChoice> choices, double[] base, List<String> warnings) {
        List<Map<String, Integer>> indexes = new ArrayList<>();
        List<List<double[]>> batches = new ArrayList<>();
        for (DayChoice c : choices) {
            Map<String, Integer> index = new LinkedHashMap<>();
            List<double[]> points = new ArrayList<>();
            List<double[]> candidates = new ArrayList<>();
            candidates.add(base);
            for (Place p : c.sights()) {
                candidates.add(p.point());
            }
            candidates.add(c.lunch() == null ? null : c.lunch().point());
            candidates.add(c.dinner() == null ? null : c.dinner().point());
            for (double[] p : candidates) {
                if (p != null && !index.containsKey(DayTravel.key(p))) {
                    index.put(DayTravel.key(p), points.size());
                    points.add(p);
                }
            }
            indexes.add(index);
            batches.add(points);
        }
        List<Optional<RoutingService.Matrix>> matrices = routingService.isEnabled()
                ? routingService.matrices(batches) : null;
        List<DayTravel> travels = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            Optional<RoutingService.Matrix> m = matrices == null ? Optional.empty() : matrices.get(i);
            if (m.isPresent()) {
                travels.add(new DayTravel(indexes.get(i), m.get()));
            } else {
                if (batches.get(i).size() >= 2) {
                    warnings.add("routing_estimated");
                }
                travels.add(DayTravel.estimate());
            }
        }
        return travels;
    }

    private PlannerDayDTO schedule(int dayNumber, LocalDate date, DayChoice choice, PlannerStopDTO hotelStop,
                                   double[] base, DayTravel travel, Options opt) {
        PlannerDayDTO day = new PlannerDayDTO();
        day.setDayNumber(dayNumber);
        day.setDate(date);
        day.setTheme(choice.theme());
        day.setRouting(travel.routed() ? "osrm" : "estimate");

        // Chợ đêm / phố đi bộ để sau bữa tối; còn lại: đường ngắn nhất từ khách sạn, điểm không toạ độ để cuối.
        List<Place> daytime = new ArrayList<>();
        List<Place> night = new ArrayList<>();
        for (Place p : choice.sights()) {
            (NIGHT.matcher(StringUtil.removeAccents(p.name())).find() ? night : daytime).add(p);
        }
        List<Place> ordered = route(base, daytime, travel);

        int clock = switch (opt.pace()) {
            case "relaxed" -> 9 * 60;
            case "packed" -> 8 * 60;
            default -> 8 * 60 + 30;
        };
        double[] prev = base;
        double dayKm = 0;
        if (dayNumber == 1 && hotelStop != null) {
            PlannerStopDTO h = copyHotel(hotelStop, opt);
            h.setStartTime(time(clock));
            clock += 30;
            h.setEndTime(time(clock));
            day.getItems().add(h);
        }
        int morning = Math.max(1, (ordered.size() + 1) / 2);
        boolean lunchDone = false;
        for (int i = 0; i < ordered.size(); i++) {
            if (!lunchDone && i > 0 && (i >= morning || clock >= 11 * 60 + 30)) {
                double[] next = mealStop(day, choice.lunch(), "lunch", prev, clock, choice.notes(), travel, opt);
                clock = lastEnd(day);
                dayKm += lastKm(day);
                prev = next != null ? next : prev;
                lunchDone = true;
            }
            Place p = ordered.get(i);
            dayKm += addSight(day, p, prev, clock, 0, choice.notes(), travel, opt);
            clock = lastEnd(day);
            if (p.point() != null) {
                prev = p.point();
            }
        }
        if (!lunchDone) {
            if (ordered.isEmpty()) {
                clock = Math.max(clock, 12 * 60 - 30);
            }
            double[] next = mealStop(day, choice.lunch(), "lunch", prev, clock, choice.notes(), travel, opt);
            clock = lastEnd(day);
            dayKm += lastKm(day);
            prev = next != null ? next : prev;
        }
        double[] next = mealStop(day, choice.dinner(), "dinner", prev, clock, choice.notes(), travel, opt);
        dayKm += lastKm(day);
        clock = lastEnd(day);
        prev = next != null ? next : prev;
        for (Place p : route(prev, night, travel)) {
            dayKm += addSight(day, p, prev, clock, 19 * 60 + 30, choice.notes(), travel, opt);
            clock = lastEnd(day);
            if (p.point() != null) {
                prev = p.point();
            }
        }
        day.setDistanceKm(round1(dayKm));
        return day;
    }

    /** Đường nhanh nhất qua các điểm có toạ độ (xuất phát từ start), điểm không toạ độ nối vào cuối. */
    private List<Place> route(double[] start, List<Place> places, DayTravel travel) {
        List<Place> located = new ArrayList<>();
        List<Place> unlocated = new ArrayList<>();
        for (Place p : places) {
            (p.point() != null ? located : unlocated).add(p);
        }
        int n = located.size();
        double[] fromStart = new double[n];
        double[][] cost = new double[n][n];
        for (int i = 0; i < n; i++) {
            fromStart[i] = start == null ? 0 : travel.cost(start, located.get(i).point());
            for (int j = 0; j < n; j++) {
                cost[i][j] = i == j ? 0 : travel.cost(located.get(i).point(), located.get(j).point());
            }
        }
        List<Place> ordered = new ArrayList<>();
        for (int i : GeoPlanning.bestOpenPath(fromStart, cost)) {
            ordered.add(located.get(i));
        }
        ordered.addAll(unlocated);
        return ordered;
    }

    /**
     * Thêm điểm tham quan bắt đầu sau clock + thời gian di chuyển (không sớm hơn earliest, chờ tới giờ mở cửa nếu đọc được).
     * Trả số km đã đi từ điểm trước.
     */
    private double addSight(PlannerDayDTO day, Place p, double[] prev, int clock, int earliest,
                            Map<UUID, String> notes, DayTravel dayTravel, Options opt) {
        PlannerStopDTO stop = placeStop(p, "sight", p.name(), notes.get(p.id()));
        Leg leg = dayTravel.leg(prev, p.point());
        int travel = leg.minutes();
        double km = leg.km() == null ? 0 : leg.km();
        stop.setDistanceKm(leg.km());
        stop.setTravelMode(leg.mode());
        stop.setTravelMinutes(travel);
        int start = Math.min(Math.max(clock + travel, earliest), LATEST_MINUTE);
        int duration = THEME_PARK.matcher(StringUtil.removeAccents(p.name())).find()
                ? 210 : visitMinutes(p.category(), opt.pace());
        int[] hours = openingHours(p.openingHours());
        if (hours != null && start < hours[0]) {
            start = hours[0];
        }
        if (hours != null && hours[1] > hours[0] && start + duration > hours[1]) {
            stop.setMayBeClosed(true);
        }
        stop.setStartTime(time(start));
        stop.setEndTime(time(Math.min(start + duration, LATEST_MINUTE)));
        if (p.entryFee() != null && p.entryFee().signum() > 0) {
            stop.setEstimatedCost(p.entryFee().multiply(BigDecimal.valueOf(opt.partySize())));
        }
        day.getItems().add(stop);
        return km;
    }

    /** Thêm bữa ăn; không có quán thì là mục "ăn tự do" không gắn địa điểm. Trả toạ độ quán (nếu có). */
    private double[] mealStop(PlannerDayDTO day, Place place, String kind, double[] prev, int clock,
                              Map<UUID, String> notes, DayTravel dayTravel, Options opt) {
        boolean lunch = "lunch".equals(kind);
        PlannerStopDTO stop;
        if (place == null) {
            stop = new PlannerStopDTO();
            stop.setKind(kind);
            stop.setTitle(opt.vi() ? (lunch ? "Ăn trưa tự do quanh khu vực" : "Ăn tối tự do quanh khu vực")
                    : (lunch ? "Lunch nearby (free choice)" : "Dinner nearby (free choice)"));
        } else {
            String label = opt.vi() ? (lunch ? "Ăn trưa: " : "Ăn tối: ") : (lunch ? "Lunch: " : "Dinner: ");
            stop = placeStop(place, kind, truncate(label + place.name(), 255), notes.get(place.id()));
            stop.setEntityType("restaurant"); // địa danh loại restaurant (ItineraryServiceImpl.referenceType)
        }
        double[] point = place == null ? null : place.point();
        Leg leg = place == null ? null : dayTravel.leg(prev, point);
        int travel = leg == null ? 0 : leg.minutes();
        if (leg != null) {
            stop.setDistanceKm(leg.km());
            stop.setTravelMode(leg.mode());
        }
        stop.setTravelMinutes(leg == null ? null : travel);
        int earliest = lunch ? 11 * 60 : 18 * 60;
        int start = Math.min(Math.max(clock + travel, earliest), LATEST_MINUTE);
        int duration = "relaxed".equals(opt.pace()) ? (lunch ? 75 : 90) : (lunch ? 60 : 75);
        stop.setStartTime(time(start));
        stop.setEndTime(time(Math.min(start + duration, LATEST_MINUTE)));
        stop.setEstimatedCost(MEAL_COST.getOrDefault(opt.budget(), MEAL_COST.get("mid"))
                .multiply(BigDecimal.valueOf(opt.partySize())));
        day.getItems().add(stop);
        return point;
    }

    private int lastEnd(PlannerDayDTO day) {
        LocalTime end = day.getItems().get(day.getItems().size() - 1).getEndTime();
        return end.getHour() * 60 + end.getMinute();
    }

    private double lastKm(PlannerDayDTO day) {
        Double km = day.getItems().get(day.getItems().size() - 1).getDistanceKm();
        return km == null ? 0 : km;
    }

    private int visitMinutes(String category, String pace) {
        int base = VISIT_MINUTES.getOrDefault(category, 60);
        double factor = "relaxed".equals(pace) ? 1.2 : "packed".equals(pace) ? 0.85 : 1;
        return (int) Math.round(base * factor / 5) * 5;
    }

    /** Đọc "HH:MM-HH:MM" đầu tiên trong chuỗi giờ mở cửa → {mở, đóng} theo phút; không đọc được → null. */
    static int[] openingHours(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = HOURS.matcher(text);
        if (!m.find()) {
            return null;
        }
        int open = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        int close = Integer.parseInt(m.group(3)) * 60 + Integer.parseInt(m.group(4));
        if (open >= 24 * 60 || close > 24 * 60) {
            return null;
        }
        return new int[]{open, close};
    }

    // ================================================================== DTO

    private PlannerStopDTO placeStop(Place p, String kind, String title, String note) {
        PlannerStopDTO stop = new PlannerStopDTO();
        stop.setKind(kind);
        stop.setEntityType("landmark");
        stop.setEntityId(p.id());
        stop.setName(p.name());
        stop.setTitle(truncate(title, 255));
        stop.setCategory(p.category());
        if (p.point() != null) {
            stop.setLatitude(p.point()[0]);
            stop.setLongitude(p.point()[1]);
        }
        stop.setAddress(p.address());
        stop.setCoverImageUrl(p.cover());
        stop.setOpeningHours(p.openingHours());
        stop.setNotes(note);
        stop.setRating(p.avgRating() == null ? null : Math.round(p.avgRating() * 10) / 10.0);
        stop.setReviewCount(p.reviewCount());
        return stop;
    }

    private PlannerStopDTO hotelStop(HotelOption h, int nights, int rooms, Options opt) {
        PlannerStopDTO stop = new PlannerStopDTO();
        stop.setKind("hotel");
        stop.setEntityType("hotel");
        stop.setEntityId(h.id());
        stop.setName(h.name());
        stop.setTitle(truncate((opt.vi() ? "Nhận phòng / gửi hành lý: " : "Check in / drop bags: ") + h.name(), 255));
        if (h.point() != null) {
            stop.setLatitude(h.point()[0]);
            stop.setLongitude(h.point()[1]);
        }
        stop.setAddress(h.address());
        stop.setCoverImageUrl(h.cover());
        stop.setStarRating(h.stars());
        stop.setPricePerNight(h.price());
        stop.setRating(h.avgRating() == null ? null : Math.round(h.avgRating() * 10) / 10.0);
        stop.setReviewCount(h.reviewCount());
        stop.setEstimatedCost(h.price().multiply(BigDecimal.valueOf((long) nights * rooms)));
        return stop;
    }

    private PlannerStopDTO copyHotel(PlannerStopDTO h, Options opt) {
        PlannerStopDTO stop = new PlannerStopDTO();
        stop.setKind(h.getKind());
        stop.setEntityType(h.getEntityType());
        stop.setEntityId(h.getEntityId());
        stop.setName(h.getName());
        stop.setTitle(h.getTitle());
        stop.setLatitude(h.getLatitude());
        stop.setLongitude(h.getLongitude());
        stop.setAddress(h.getAddress());
        stop.setCoverImageUrl(h.getCoverImageUrl());
        stop.setStarRating(h.getStarRating());
        stop.setPricePerNight(h.getPricePerNight());
        stop.setRating(h.getRating());
        stop.setReviewCount(h.getReviewCount());
        stop.setEstimatedCost(h.getEstimatedCost());
        stop.setNotes(h.getNotes());
        return stop;
    }

    private String promptText(Options opt, String note) {
        StringBuilder sb = new StringBuilder("AI Planner")
                .append(" | interests=").append(String.join(",", opt.interests()))
                .append(" | pace=").append(opt.pace())
                .append(" | budget=").append(opt.budget())
                .append(" | party=").append(opt.partySize());
        if (opt.radiusKm() != null) {
            sb.append(" | radiusKm=").append(opt.radiusKm());
        }
        if (note != null && !note.isBlank()) {
            sb.append(" | note=").append(note.trim());
        }
        return truncate(sb.toString(), 2000);
    }

    // ================================================================== tiện ích

    private void checkRateLimit(UUID userId) {
        long now = System.currentTimeMillis();
        long windowStart = now - rateLimitWindowMinutes * 60_000L;
        Deque<Long> times = recentPlans.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < windowStart) {
                times.pollFirst();
            }
            if (times.size() >= rateLimitMax) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã tạo " + rateLimitMax + " lịch trình trong "
                        + rateLimitWindowMinutes + " phút, vui lòng thử lại sau ít phút");
            }
            times.addLast(now);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            return null;
        }
        return value.asString().trim();
    }

    private static LocalTime time(int minutes) {
        int m = Math.max(0, Math.min(minutes, 23 * 60 + 59));
        return LocalTime.of(m / 60, m % 60);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof BigDecimal d ? d : new BigDecimal(value.toString());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
