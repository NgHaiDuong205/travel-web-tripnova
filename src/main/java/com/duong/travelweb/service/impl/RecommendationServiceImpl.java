package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.RecommendationDTO;
import com.duong.travelweb.service.EntityReferenceService;
import com.duong.travelweb.service.RecommendationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gợi ý "hybrid" dựa trên nội dung:
 * 1. Tín hiệu của user (yêu thích ×3, đặt chỗ ×5, lịch trình ×4, bấm kết quả tìm kiếm ×2, xem trang ×1; giảm dần
 *    theo thời gian e^(-ngày/45)) → mức quan tâm theo điểm đến + hạng sao / mức giá khách sạn hay chọn.
 * 2. Chuyến đi sắp tới (booking đã xác nhận, lịch trình chưa đi) → ưu tiên tour / xe / điểm tham quan ở đó.
 * 3. Ứng viên ở các điểm đến quan tâm nhất, chấm điểm theo mức quan tâm + chất lượng (sao, ảnh, review) + độ hợp
 *    giá / hạng sao; đa dạng hoá (tối đa 4 khách sạn mỗi điểm đến). Điểm đến tương tự = cùng quốc gia.
 * 4. Chưa có tín hiệu → phổ biến (điểm đến nổi bật nhiều khách sạn).
 * Bỏ các mục đã yêu thích / đã đặt. Lưu vào recommendations (hết hạn sau 1 giờ).
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {
    private static final String ALGORITHM = "hybrid";
    private static final int TTL_MINUTES = 60;
    private static final int MAX_DESTINATIONS = 4;
    private static final int MAX_HOTELS_PER_DESTINATION = 4;
    private static final Map<String, Integer> TYPE_LIMITS = Map.of(
            "hotel", 12, "tour", 8, "destination", 8, "landmark", 8, "car", 4);
    private static final Map<String, Double> ACTION_WEIGHTS = Map.of("view", 1.0, "click", 1.5, "share", 2.0);
    private static final int SIMILAR_CACHE_DAYS = 7;
    private static final int SIMILAR_STORED = 12;

    private final EntityReferenceService entityReferenceService;
    @PersistenceContext
    private EntityManager entityManager;

    public RecommendationServiceImpl(EntityReferenceService entityReferenceService) {
        this.entityReferenceService = entityReferenceService;
    }

    // =================================================================== gợi ý cá nhân

    @Override
    @Transactional
    public List<RecommendationDTO> recommend(UUID userId, String type, int limit) {
        String filter = type == null || type.isBlank() ? "all" : type.trim().toLowerCase();
        if (!"all".equals(filter) && !TYPES.contains(filter)) {
            throw ApiException.badRequest("type phải là all hoặc một trong: " + String.join(", ", TYPES));
        }
        if (!hasFreshRows(userId)) {
            regenerate(userId);
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("SELECT entity_type, entity_id, score, reason "
                        + "FROM recommendations WHERE user_id = :u AND algorithm = :a "
                        + "AND (CAST(:t AS text) = 'all' OR entity_type = CAST(:t AS text)) ORDER BY score DESC, entity_id")
                .setParameter("u", userId)
                .setParameter("a", ALGORITHM)
                .setParameter("t", filter)
                .getResultList();
        Map<String, List<RecommendationDTO>> byType = new LinkedHashMap<>();
        TYPES.forEach(t -> byType.put(t, new ArrayList<>()));
        for (Object[] row : rows) {
            RecommendationDTO dto = new RecommendationDTO();
            dto.setEntityType((String) row[0]);
            dto.setEntityId((UUID) row[1]);
            dto.setScore(round(((Number) row[2]).doubleValue()));
            dto.setReason((String) row[3]);
            byType.computeIfAbsent(dto.getEntityType(), k -> new ArrayList<>()).add(dto);
        }
        byType.forEach(this::enrich); // bỏ mục đã ẩn / xoá
        int max = Math.max(1, limit);
        if (!"all".equals(filter)) {
            List<RecommendationDTO> list = byType.get(filter);
            return list.subList(0, Math.min(max, list.size()));
        }
        // Xen kẽ các loại theo thứ hạng để trang chủ có đủ khách sạn / tour / điểm đến.
        List<RecommendationDTO> mixed = new ArrayList<>();
        for (int rank = 0; mixed.size() < max; rank++) {
            boolean any = false;
            for (List<RecommendationDTO> list : byType.values()) {
                if (rank < list.size() && mixed.size() < max) {
                    mixed.add(list.get(rank));
                    any = true;
                }
            }
            if (!any) {
                break;
            }
        }
        return mixed;
    }

    @Override
    @Transactional
    public void invalidate(UUID userId) {
        entityManager.createNativeQuery("UPDATE recommendations SET expires_at = now() WHERE user_id = :u AND expires_at > now()")
                .setParameter("u", userId)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void recordInteraction(UUID userId, String entityType, UUID entityId, String action) {
        String type = entityType == null ? null : entityType.trim().toLowerCase();
        if (!EntityReferenceService.REVIEWABLE_TYPES.contains(type)) {
            throw ApiException.badRequest("entityType phải là một trong: " + String.join(", ", EntityReferenceService.REVIEWABLE_TYPES));
        }
        String act = action == null || action.isBlank() ? "view" : action.trim().toLowerCase();
        Double weight = ACTION_WEIGHTS.get(act);
        if (weight == null) {
            throw ApiException.badRequest("action phải là view, click hoặc share");
        }
        int inserted = entityManager.createNativeQuery("INSERT INTO user_interactions (user_id, entity_type, entity_id, action, weight, created_at) "
                        + "SELECT :u, :t, :e, :a, :w, now() WHERE NOT EXISTS (SELECT 1 FROM user_interactions "
                        + "WHERE user_id = :u AND entity_type = :t AND entity_id = :e AND action = :a "
                        + "AND created_at > now() - interval '30 minutes')")
                .setParameter("u", userId)
                .setParameter("t", type)
                .setParameter("e", entityId)
                .setParameter("a", act)
                .setParameter("w", weight)
                .executeUpdate();
        if (inserted > 0) {
            // Có tín hiệu mới: gợi ý hiện tại chỉ dùng tối đa 5 phút kể từ lúc tính (không tính lại sau mỗi lượt xem).
            entityManager.createNativeQuery("UPDATE recommendations SET expires_at = generated_at + interval '5 minutes' "
                            + "WHERE user_id = :u AND expires_at > generated_at + interval '5 minutes'")
                    .setParameter("u", userId)
                    .executeUpdate();
        }
    }

    private boolean hasFreshRows(UUID userId) {
        Number n = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM recommendations "
                        + "WHERE user_id = :u AND algorithm = :a AND expires_at > now()")
                .setParameter("u", userId)
                .setParameter("a", ALGORITHM)
                .getSingleResult();
        return n.longValue() > 0;
    }

    private record Signal(UUID destinationId, String type, UUID id, double weight, String source, Integer stars,
                          BigDecimal hotelPrice) {
    }

    private record Candidate(String type, UUID id, UUID destinationId, double score, String reason) {
    }

    private void regenerate(UUID userId) {
        // 2 request cùng lúc của 1 user không cùng tính lại.
        entityManager.createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(CAST(:k AS text)))) x")
                .setParameter("k", "rec:" + userId)
                .getResultList();
        if (hasFreshRows(userId)) {
            return;
        }
        List<Signal> signals = loadSignals(userId);
        Map<UUID, Double> affinity = new HashMap<>();
        Set<String> excluded = new HashSet<>();
        double starSum = 0, starWeight = 0, logPriceSum = 0, priceWeight = 0;
        for (Signal s : signals) {
            if (s.destinationId() != null) {
                affinity.merge(s.destinationId(), s.weight(), Double::sum);
            }
            if ("favorite".equals(s.source()) || "booking".equals(s.source())) {
                excluded.add(s.type() + ":" + s.id());
            }
            if ("hotel".equals(s.type())) {
                if (s.stars() != null) {
                    starSum += s.stars() * s.weight();
                    starWeight += s.weight();
                }
                if (s.hotelPrice() != null && s.hotelPrice().signum() > 0) {
                    logPriceSum += Math.log(s.hotelPrice().doubleValue()) * s.weight();
                    priceWeight += s.weight();
                }
            }
        }
        Double preferredStars = starWeight > 0 ? starSum / starWeight : null;
        Double preferredPrice = priceWeight > 0 ? Math.exp(logPriceSum / priceWeight) : null;
        Set<UUID> upcoming = upcomingDestinations(userId);
        boolean coldStart = affinity.isEmpty() && upcoming.isEmpty();

        // Mức quan tâm chuẩn hoá 0..1; chuyến sắp đi = 1.
        Map<UUID, Double> weightOf = new LinkedHashMap<>();
        upcoming.forEach(d -> weightOf.put(d, 1.0));
        double maxAffinity = affinity.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        affinity.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(MAX_DESTINATIONS)
                .forEach(e -> weightOf.putIfAbsent(e.getKey(), 0.3 + 0.6 * e.getValue() / maxAffinity));
        List<UUID> popular = popularDestinations(8);
        if (coldStart) {
            popular.stream().limit(MAX_DESTINATIONS).forEach(d -> weightOf.put(d, 0.6));
        }
        java.util.function.Function<UUID, String> reasonOf = d -> upcoming.contains(d) ? "upcoming_trip"
                : coldStart ? "popular" : "liked_destination";

        List<Candidate> picked = new ArrayList<>();
        Set<UUID> dests = weightOf.keySet();
        picked.addAll(hotelCandidates(userId, dests, weightOf, reasonOf, preferredStars, preferredPrice, excluded));
        picked.addAll(tourCandidates(dests, weightOf, reasonOf, excluded));
        picked.addAll(destinationCandidates(affinity.keySet(), upcoming, popular, coldStart));
        Set<UUID> sightDests = upcoming.isEmpty() ? new LinkedHashSet<>(dests.stream().limit(2).toList()) : upcoming;
        picked.addAll(landmarkCandidates(sightDests, weightOf, reasonOf));
        if (!upcoming.isEmpty()) {
            picked.addAll(carCandidates(upcoming, excluded));
        }

        entityManager.createNativeQuery("DELETE FROM recommendations WHERE user_id = :u AND algorithm = :a")
                .setParameter("u", userId)
                .setParameter("a", ALGORITHM)
                .executeUpdate();
        for (Candidate c : picked) {
            entityManager.createNativeQuery("INSERT INTO recommendations (user_id, entity_type, entity_id, score, reason, "
                            + "algorithm, generated_at, expires_at) VALUES (:u, :t, :e, :s, :r, :a, now(), "
                            + "now() + make_interval(mins => :ttl)) ON CONFLICT (user_id, entity_type, entity_id, algorithm) "
                            + "DO UPDATE SET score = GREATEST(recommendations.score, EXCLUDED.score), expires_at = EXCLUDED.expires_at")
                    .setParameter("u", userId)
                    .setParameter("t", c.type())
                    .setParameter("e", c.id())
                    .setParameter("s", (float) c.score())
                    .setParameter("r", c.reason())
                    .setParameter("a", ALGORITHM)
                    .setParameter("ttl", TTL_MINUTES)
                    .executeUpdate();
        }
        if (picked.isEmpty()) {
            // Không có gì để gợi ý (DB trống): vẫn ghi dấu để không tính lại mỗi request.
            entityManager.createNativeQuery("INSERT INTO recommendations (user_id, entity_type, entity_id, score, reason, "
                            + "algorithm, generated_at, expires_at) VALUES (:u, 'none', :u, 0, 'empty', :a, now(), "
                            + "now() + make_interval(mins => :ttl))")
                    .setParameter("u", userId)
                    .setParameter("a", ALGORITHM)
                    .setParameter("ttl", TTL_MINUTES)
                    .executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Signal> loadSignals(UUID userId) {
        List<Object[]> rows = entityManager.createNativeQuery("WITH sig AS ("
                        + "SELECT CAST(f.item_type AS text) AS et, f.item_id AS eid, 3.0 AS w, f.created_at AS at, 'favorite' AS src "
                        + "FROM favorites f WHERE f.user_id = :u "
                        + "UNION ALL SELECT 'hotel', b.hotel_id, 5.0, b.created_at, 'booking' FROM hotel_bookings b "
                        + "WHERE b.user_id = :u AND b.status NOT IN ('cancelled', 'refunded') "
                        + "UNION ALL SELECT 'tour', b.tour_id, 5.0, b.created_at, 'booking' FROM tour_bookings b "
                        + "WHERE b.user_id = :u AND b.status NOT IN ('cancelled', 'refunded') "
                        + "UNION ALL SELECT 'car', b.car_id, 4.0, b.created_at, 'booking' FROM car_bookings b "
                        + "WHERE b.user_id = :u AND b.status NOT IN ('cancelled', 'refunded') "
                        + "UNION ALL SELECT 'flight', b.flight_id, 4.0, b.created_at, 'booking' FROM flight_bookings b "
                        + "WHERE b.user_id = :u AND b.status NOT IN ('cancelled', 'refunded') "
                        + "UNION ALL SELECT s.clicked_entity_type, s.clicked_entity_id, 2.0, s.created_at, 'search' "
                        + "FROM search_queries s WHERE s.user_id = :u AND s.clicked_entity_id IS NOT NULL "
                        + "UNION ALL SELECT i.entity_type, i.entity_id, COALESCE(i.weight, 1.0), i.created_at, 'view' "
                        + "FROM user_interactions i WHERE i.user_id = :u AND i.created_at > now() - interval '180 days' "
                        + "UNION ALL SELECT 'destination', it.destination_id, 4.0, it.created_at, 'itinerary' "
                        + "FROM itineraries it WHERE it.user_id = :u AND it.destination_id IS NOT NULL) "
                        + "SELECT COALESCE(h.destination_id, t.destination_id, c.destination_id, l.destination_id, fd.id, "
                        + "CASE WHEN sig.et = 'destination' THEN sig.eid END), sig.et, sig.eid, "
                        + "sig.w * exp(-GREATEST(EXTRACT(EPOCH FROM now() - sig.at), 0) / 86400.0 / 45.0), sig.src, "
                        + "h.star_rating, (SELECT MIN(rt.price_per_night) FROM room_types rt WHERE rt.hotel_id = h.id AND rt.is_active) "
                        + "FROM sig LEFT JOIN hotels h ON sig.et = 'hotel' AND h.id = sig.eid "
                        + "LEFT JOIN tours t ON sig.et = 'tour' AND t.id = sig.eid "
                        + "LEFT JOIN cars c ON sig.et = 'car' AND c.id = sig.eid "
                        + "LEFT JOIN landmarks l ON sig.et = 'landmark' AND l.id = sig.eid "
                        + "LEFT JOIN flights f ON sig.et = 'flight' AND f.id = sig.eid "
                        + "LEFT JOIN LATERAL (SELECT d.id FROM destinations d WHERE f.id IS NOT NULL AND d.is_active "
                        + "AND lower(unaccent(d.name)) = lower(unaccent(f.arrival_city)) LIMIT 1) fd ON true")
                .setParameter("u", userId)
                .getResultList();
        List<Signal> signals = new ArrayList<>();
        for (Object[] r : rows) {
            signals.add(new Signal((UUID) r[0], (String) r[1], (UUID) r[2], ((Number) r[3]).doubleValue(), (String) r[4],
                    r[5] == null ? null : ((Number) r[5]).intValue(), (BigDecimal) r[6]));
        }
        return signals;
    }

    @SuppressWarnings("unchecked")
    private Set<UUID> upcomingDestinations(UUID userId) {
        List<UUID> rows = entityManager.createNativeQuery("SELECT h.destination_id FROM hotel_bookings b "
                        + "JOIN hotels h ON h.id = b.hotel_id WHERE b.user_id = :u AND b.status IN ('confirmed', 'checked_in') "
                        + "AND b.check_out_date >= CURRENT_DATE "
                        + "UNION SELECT t.destination_id FROM tour_bookings b JOIN tours t ON t.id = b.tour_id "
                        + "WHERE b.user_id = :u AND b.status IN ('confirmed', 'checked_in') AND b.departure_date >= CURRENT_DATE "
                        + "UNION SELECT it.destination_id FROM itineraries it WHERE it.user_id = :u "
                        + "AND it.destination_id IS NOT NULL AND it.end_date >= CURRENT_DATE")
                .setParameter("u", userId)
                .getResultList();
        Set<UUID> set = new LinkedHashSet<>();
        rows.stream().filter(java.util.Objects::nonNull).limit(3).forEach(set::add);
        return set;
    }

    @SuppressWarnings("unchecked")
    private List<UUID> popularDestinations(int limit) {
        return entityManager.createNativeQuery("SELECT d.id FROM destinations d WHERE d.is_active "
                        + "ORDER BY d.is_popular DESC, (SELECT COUNT(*) FROM hotels h WHERE h.destination_id = d.id AND h.is_active) DESC, "
                        + "(d.cover_image_url IS NOT NULL) DESC, d.id LIMIT :n")
                .setParameter("n", limit)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Candidate> hotelCandidates(UUID userId, Set<UUID> dests, Map<UUID, Double> weightOf,
                                            java.util.function.Function<UUID, String> reasonOf,
                                            Double preferredStars, Double preferredPrice, Set<String> excluded) {
        if (dests.isEmpty()) {
            return List.of();
        }
        // Lấy trước ≤ 60 khách sạn tốt nhất mỗi điểm đến (có ảnh, nhiều sao; xoay vòng theo ngày cho đa dạng).
        List<Object[]> rows = entityManager.createNativeQuery("SELECT x.id, x.destination_id, x.star_rating, x.has_img, "
                        + "(SELECT MIN(rt.price_per_night) FROM room_types rt WHERE rt.hotel_id = x.id AND rt.is_active), "
                        + "r.avg_rating, r.cnt FROM (SELECT h.id, h.destination_id, h.star_rating, "
                        + "(h.cover_image_url IS NOT NULL) AS has_img, ROW_NUMBER() OVER (PARTITION BY h.destination_id "
                        + "ORDER BY (h.cover_image_url IS NOT NULL) DESC, h.star_rating DESC NULLS LAST, "
                        + "md5(CAST(h.id AS text) || :seed)) AS rn FROM hotels h WHERE h.is_active AND h.destination_id IN (:d)) x "
                        + "LEFT JOIN LATERAL (SELECT AVG(p.rating) AS avg_rating, COUNT(*) AS cnt FROM posts p "
                        + "WHERE p.entity_type = 'hotel' AND p.entity_id = x.id AND p.status = 'approved') r ON true "
                        + "WHERE x.rn <= 60")
                .setParameter("d", dests)
                .setParameter("seed", userId + ":" + java.time.LocalDate.now())
                .getResultList();
        List<Candidate> scored = new ArrayList<>();
        for (Object[] r : rows) {
            UUID id = (UUID) r[0];
            if (excluded.contains("hotel:" + id)) {
                continue;
            }
            UUID dest = (UUID) r[1];
            Integer stars = r[2] == null ? null : ((Number) r[2]).intValue();
            boolean img = Boolean.TRUE.equals(r[3]);
            BigDecimal price = (BigDecimal) r[4];
            double starTerm = stars == null ? 0.5 : stars / 5.0;
            double starFit = preferredStars == null || stars == null ? 0.5 : 1 - Math.abs(stars - preferredStars) / 4.0;
            double priceFit = preferredPrice == null || price == null || price.signum() <= 0 ? 0.5
                    : Math.exp(-Math.abs(Math.log(price.doubleValue() / preferredPrice)));
            double score = 0.4 * weightOf.getOrDefault(dest, 0.3) + 0.15 * starTerm + 0.1 * starFit + 0.15 * priceFit
                    + 0.1 * (img ? 1 : 0) + 0.1 * reviewScore(r[5], r[6]);
            scored.add(new Candidate("hotel", id, dest, score, reasonOf.apply(dest)));
        }
        return diversify(scored, TYPE_LIMITS.get("hotel"), MAX_HOTELS_PER_DESTINATION);
    }

    @SuppressWarnings("unchecked")
    private List<Candidate> tourCandidates(Set<UUID> dests, Map<UUID, Double> weightOf,
                                           java.util.function.Function<UUID, String> reasonOf, Set<String> excluded) {
        List<Object[]> rows = entityManager.createNativeQuery("SELECT t.id, t.destination_id, (t.cover_image_url IS NOT NULL), "
                        + "r.avg_rating, r.cnt FROM tours t LEFT JOIN LATERAL (SELECT AVG(p.rating) AS avg_rating, COUNT(*) AS cnt "
                        + "FROM posts p WHERE p.entity_type = 'tour' AND p.entity_id = t.id AND p.status = 'approved') r ON true "
                        + "WHERE t.is_active AND (t.departure_date IS NULL OR t.departure_date >= CURRENT_DATE)")
                .getResultList();
        List<Candidate> scored = new ArrayList<>();
        for (Object[] r : rows) {
            UUID id = (UUID) r[0];
            if (excluded.contains("tour:" + id)) {
                continue;
            }
            UUID dest = (UUID) r[1];
            boolean match = dests.contains(dest);
            double score = 0.5 * (match ? weightOf.get(dest) : 0.1) + 0.2 * (Boolean.TRUE.equals(r[2]) ? 1 : 0)
                    + 0.2 * reviewScore(r[3], r[4]) + 0.1;
            scored.add(new Candidate("tour", id, dest, score, match ? reasonOf.apply(dest) : "popular"));
        }
        return diversify(scored, TYPE_LIMITS.get("tour"), Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    private List<Candidate> destinationCandidates(Set<UUID> liked, Set<UUID> upcoming, List<UUID> popular, boolean coldStart) {
        List<Candidate> out = new ArrayList<>();
        Set<UUID> known = new HashSet<>(liked);
        known.addAll(upcoming);
        if (!coldStart && !known.isEmpty()) {
            // Cùng quốc gia với nơi đã quan tâm, chưa từng quan tâm: nổi bật + nhiều khách sạn trước.
            List<Object[]> rows = entityManager.createNativeQuery("SELECT d.id, d.is_popular, (d.cover_image_url IS NOT NULL), "
                            + "(SELECT COUNT(*) FROM hotels h WHERE h.destination_id = d.id AND h.is_active) AS n "
                            + "FROM destinations d WHERE d.is_active AND d.id NOT IN (:known) "
                            + "AND d.country_id IN (SELECT country_id FROM destinations WHERE id IN (:known)) "
                            + "ORDER BY d.is_popular DESC, n DESC, d.id LIMIT 20")
                    .setParameter("known", known)
                    .getResultList();
            for (Object[] r : rows) {
                double hotels = ((Number) r[3]).doubleValue();
                double score = 0.4 * (Boolean.TRUE.equals(r[1]) ? 1 : 0.5) + 0.4 * Math.min(1, Math.log1p(hotels) / Math.log(2000))
                        + 0.2 * (Boolean.TRUE.equals(r[2]) ? 1 : 0);
                out.add(new Candidate("destination", (UUID) r[0], (UUID) r[0], score, "similar_destination"));
            }
        }
        int rank = 0;
        for (UUID id : popular) {
            if (!known.contains(id) && out.stream().noneMatch(c -> c.id().equals(id))) {
                out.add(new Candidate("destination", id, id, 0.5 - 0.02 * rank++, "popular"));
            }
        }
        return diversify(out, TYPE_LIMITS.get("destination"), Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    private List<Candidate> landmarkCandidates(Set<UUID> dests, Map<UUID, Double> weightOf,
                                               java.util.function.Function<UUID, String> reasonOf) {
        if (dests.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = entityManager.createNativeQuery("SELECT x.id, x.destination_id, x.has_img, r.avg_rating, r.cnt "
                        + "FROM (SELECT l.id, l.destination_id, (l.cover_image_url IS NOT NULL) AS has_img, "
                        + "ROW_NUMBER() OVER (PARTITION BY l.destination_id ORDER BY (l.cover_image_url IS NOT NULL) DESC, "
                        + "(l.description IS NOT NULL) DESC, l.id) AS rn FROM landmarks l WHERE l.is_active "
                        + "AND l.destination_id IN (:d) AND CAST(l.category AS text) NOT IN ('restaurant', 'market', 'wellness', 'sport')) x "
                        + "LEFT JOIN LATERAL (SELECT AVG(p.rating) AS avg_rating, COUNT(*) AS cnt FROM posts p "
                        + "WHERE p.entity_type = 'landmark' AND p.entity_id = x.id AND p.status = 'approved') r ON true WHERE x.rn <= 20")
                .setParameter("d", dests)
                .getResultList();
        List<Candidate> scored = new ArrayList<>();
        for (Object[] r : rows) {
            UUID dest = (UUID) r[1];
            double score = 0.5 * weightOf.getOrDefault(dest, 0.3) + 0.3 * (Boolean.TRUE.equals(r[2]) ? 1 : 0)
                    + 0.2 * reviewScore(r[3], r[4]);
            scored.add(new Candidate("landmark", (UUID) r[0], dest, score, reasonOf.apply(dest)));
        }
        return diversify(scored, TYPE_LIMITS.get("landmark"), 6);
    }

    @SuppressWarnings("unchecked")
    private List<Candidate> carCandidates(Set<UUID> dests, Set<String> excluded) {
        List<Object[]> rows = entityManager.createNativeQuery("SELECT c.id, c.destination_id, (c.cover_image_url IS NOT NULL), "
                        + "c.price_per_day FROM cars c WHERE c.is_active AND c.destination_id IN (:d) ORDER BY c.price_per_day LIMIT 20")
                .setParameter("d", dests)
                .getResultList();
        List<Candidate> scored = new ArrayList<>();
        int rank = 0;
        for (Object[] r : rows) {
            if (excluded.contains("car:" + r[0])) {
                continue;
            }
            double score = 0.5 + 0.3 * (Boolean.TRUE.equals(r[2]) ? 1 : 0) + 0.2 * Math.max(0, 1 - rank++ / 10.0);
            scored.add(new Candidate("car", (UUID) r[0], (UUID) r[1], score, "upcoming_trip"));
        }
        return diversify(scored, TYPE_LIMITS.get("car"), Integer.MAX_VALUE);
    }

    /** Điểm review 0..1: điểm TB / 5, giảm khi ít review (đủ tin cậy từ ~20 review). */
    private static double reviewScore(Object avg, Object count) {
        long n = count == null ? 0 : ((Number) count).longValue();
        if (n == 0 || avg == null) {
            return 0;
        }
        return ((Number) avg).doubleValue() / 5.0 * Math.min(1, Math.log1p(n) / Math.log(20));
    }

    /** Lấy theo điểm giảm dần, mỗi điểm đến tối đa perDestination mục. */
    private static List<Candidate> diversify(List<Candidate> scored, int limit, int perDestination) {
        List<Candidate> sorted = new ArrayList<>(scored);
        sorted.sort(Comparator.comparingDouble(Candidate::score).reversed().thenComparing(c -> c.id().toString()));
        Map<UUID, Integer> perDest = new HashMap<>();
        List<Candidate> out = new ArrayList<>();
        for (Candidate c : sorted) {
            if (out.size() >= limit) {
                break;
            }
            int used = c.destinationId() == null ? 0 : perDest.getOrDefault(c.destinationId(), 0);
            if (used >= perDestination) {
                continue;
            }
            if (c.destinationId() != null) {
                perDest.put(c.destinationId(), used + 1);
            }
            out.add(c);
        }
        return out;
    }

    // =================================================================== khách sạn tương tự

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<RecommendationDTO> similarHotels(UUID hotelId, int limit) {
        Number active = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM hotels WHERE id = :h AND is_active")
                .setParameter("h", hotelId)
                .getSingleResult();
        if (active.longValue() == 0) {
            throw ApiException.notFound("Không tìm thấy khách sạn");
        }
        List<Object[]> cached = loadSimilar(hotelId);
        if (cached.isEmpty()) {
            entityManager.createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(CAST(:k AS text)))) x")
                    .setParameter("k", "similar:" + hotelId)
                    .getResultList();
            cached = loadSimilar(hotelId);
            if (cached.isEmpty()) {
                computeSimilar(hotelId);
                cached = loadSimilar(hotelId);
            }
        }
        List<RecommendationDTO> list = new ArrayList<>();
        for (Object[] r : cached) {
            RecommendationDTO dto = new RecommendationDTO();
            dto.setEntityType("hotel");
            dto.setEntityId((UUID) r[0]);
            dto.setScore(round(((Number) r[1]).doubleValue()));
            dto.setReason("similar");
            list.add(dto);
        }
        enrich("hotel", list);
        List<Object[]> base = entityManager.createNativeQuery("SELECT latitude, longitude FROM hotels WHERE id = :h")
                .setParameter("h", hotelId)
                .getResultList();
        Object[] b = base.get(0);
        if (b[0] != null && b[1] != null) {
            Map<UUID, double[]> coords = coordinates(list.stream().map(RecommendationDTO::getEntityId).toList());
            for (RecommendationDTO dto : list) {
                double[] c = coords.get(dto.getEntityId());
                if (c != null) {
                    dto.setDistanceKm(Math.round(haversine(((Number) b[0]).doubleValue(), ((Number) b[1]).doubleValue(),
                            c[0], c[1]) * 10) / 10.0);
                }
            }
        }
        return list.subList(0, Math.min(Math.max(1, limit), list.size()));
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> loadSimilar(UUID hotelId) {
        return entityManager.createNativeQuery("SELECT similar_entity_id, similarity_score FROM similar_items "
                        + "WHERE entity_type = 'hotel' AND entity_id = :h "
                        + "AND generated_at > now() - make_interval(days => :days) ORDER BY similarity_score DESC, similar_entity_id")
                .setParameter("h", hotelId)
                .setParameter("days", SIMILAR_CACHE_DAYS)
                .getResultList();
    }

    /**
     * Cùng điểm đến (và trong ~20 km nếu có toạ độ), ≤ 400 ứng viên gần hạng sao nhất. Điểm = 0.3 trùng tiện nghi
     * (Jaccard) + 0.25 gần giá + 0.2 gần hạng sao + 0.2 gần (e^(-km/5)) + 0.05 có ảnh; thiếu dữ liệu = 0.5.
     */
    @SuppressWarnings("unchecked")
    private void computeSimilar(UUID hotelId) {
        List<Object[]> rows = entityManager.createNativeQuery("WITH b AS (SELECT h.id, h.destination_id, h.star_rating, "
                        + "h.latitude, h.longitude, (SELECT MIN(rt.price_per_night) FROM room_types rt "
                        + "WHERE rt.hotel_id = h.id AND rt.is_active) AS price FROM hotels h WHERE h.id = :h), "
                        + "ba AS (SELECT amenity_id FROM hotel_amenities WHERE hotel_id = :h), "
                        + "cand AS (SELECT c.id, c.star_rating, c.latitude, c.longitude, (c.cover_image_url IS NOT NULL) AS img "
                        + "FROM hotels c, b WHERE c.destination_id = b.destination_id AND c.is_active AND c.id <> b.id "
                        + "AND (b.latitude IS NULL OR c.latitude IS NULL OR (abs(c.latitude - b.latitude) < 0.2 "
                        + "AND abs(c.longitude - b.longitude) < 0.2)) "
                        + "ORDER BY abs(COALESCE(c.star_rating, 3) - COALESCE(b.star_rating, 3)), c.id LIMIT 400) "
                        + "SELECT cand.id, cand.star_rating, cand.img, (SELECT MIN(rt.price_per_night) FROM room_types rt "
                        + "WHERE rt.hotel_id = cand.id AND rt.is_active), "
                        + "CASE WHEN b.latitude IS NULL OR cand.latitude IS NULL THEN NULL ELSE 6371 * 2 * asin(sqrt("
                        + "power(sin(radians(cand.latitude - b.latitude) / 2), 2) + cos(radians(b.latitude)) "
                        + "* cos(radians(cand.latitude)) * power(sin(radians(cand.longitude - b.longitude) / 2), 2))) END, "
                        + "(SELECT COUNT(*) FROM hotel_amenities ha WHERE ha.hotel_id = cand.id "
                        + "AND ha.amenity_id IN (SELECT amenity_id FROM ba)), "
                        + "(SELECT COUNT(*) FROM hotel_amenities ha WHERE ha.hotel_id = cand.id), "
                        + "(SELECT COUNT(*) FROM ba), b.star_rating, b.price FROM cand, b")
                .setParameter("h", hotelId)
                .getResultList();
        List<Candidate> scored = new ArrayList<>();
        for (Object[] r : rows) {
            Integer stars = r[1] == null ? null : ((Number) r[1]).intValue();
            BigDecimal price = (BigDecimal) r[3];
            Double dist = r[4] == null ? null : ((Number) r[4]).doubleValue();
            long inter = ((Number) r[5]).longValue();
            long count = ((Number) r[6]).longValue();
            long baseCount = ((Number) r[7]).longValue();
            Integer baseStars = r[8] == null ? null : ((Number) r[8]).intValue();
            BigDecimal basePrice = (BigDecimal) r[9];
            long union = count + baseCount - inter;
            double jaccard = union == 0 ? 0.5 : (double) inter / union;
            double priceSim = price == null || basePrice == null || price.signum() <= 0 || basePrice.signum() <= 0 ? 0.5
                    : Math.exp(-1.5 * Math.abs(Math.log(price.doubleValue() / basePrice.doubleValue())));
            double starSim = stars == null || baseStars == null ? 0.5 : 1 - Math.abs(stars - baseStars) / 4.0;
            double distSim = dist == null ? 0.5 : Math.exp(-dist / 5.0);
            double score = 0.3 * jaccard + 0.25 * priceSim + 0.2 * starSim + 0.2 * distSim
                    + 0.05 * (Boolean.TRUE.equals(r[2]) ? 1 : 0);
            scored.add(new Candidate("hotel", (UUID) r[0], null, score, "similar"));
        }
        scored.sort(Comparator.comparingDouble(Candidate::score).reversed().thenComparing(c -> c.id().toString()));
        entityManager.createNativeQuery("DELETE FROM similar_items WHERE entity_type = 'hotel' AND entity_id = :h")
                .setParameter("h", hotelId)
                .executeUpdate();
        for (Candidate c : scored.subList(0, Math.min(SIMILAR_STORED, scored.size()))) {
            entityManager.createNativeQuery("INSERT INTO similar_items (entity_type, entity_id, similar_entity_id, "
                            + "similarity_score, generated_at) VALUES ('hotel', :h, :s, :score, now())")
                    .setParameter("h", hotelId)
                    .setParameter("s", c.id())
                    .setParameter("score", (float) c.score())
                    .executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, double[]> coordinates(Collection<UUID> ids) {
        Map<UUID, double[]> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        List<Object[]> rows = entityManager.createNativeQuery("SELECT id, latitude, longitude FROM hotels "
                        + "WHERE id IN (:ids) AND latitude IS NOT NULL AND longitude IS NOT NULL")
                .setParameter("ids", ids)
                .getResultList();
        for (Object[] r : rows) {
            map.put((UUID) r[0], new double[]{((Number) r[1]).doubleValue(), ((Number) r[2]).doubleValue()});
        }
        return map;
    }

    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(a));
    }

    // =================================================================== thông tin hiển thị

    /** Điền tên / ảnh / giá... theo lô; bỏ mục đã ẩn hoặc xoá (giữ thứ tự). */
    @SuppressWarnings("unchecked")
    private void enrich(String type, List<RecommendationDTO> items) {
        if (items.isEmpty()) {
            return;
        }
        String sql = switch (type) {
            case "hotel" -> "SELECT h.id, h.name, h.cover_image_url, h.destination_id, d.name, "
                    + "(SELECT MIN(rt.price_per_night) FROM room_types rt WHERE rt.hotel_id = h.id AND rt.is_active), "
                    + "h.star_rating, NULL, NULL FROM hotels h LEFT JOIN destinations d ON d.id = h.destination_id "
                    + "WHERE h.id IN (:ids) AND h.is_active";
            case "tour" -> "SELECT t.id, t.name, t.cover_image_url, t.destination_id, d.name, t.price_adult, NULL, "
                    + "t.duration_days, NULL FROM tours t LEFT JOIN destinations d ON d.id = t.destination_id "
                    + "WHERE t.id IN (:ids) AND t.is_active";
            case "destination" -> "SELECT d.id, d.name, d.cover_image_url, d.id, c.name, NULL, NULL, NULL, NULL "
                    + "FROM destinations d LEFT JOIN countries c ON c.id = d.country_id WHERE d.id IN (:ids) AND d.is_active";
            case "landmark" -> "SELECT l.id, l.name, l.cover_image_url, l.destination_id, d.name, l.entry_fee, NULL, NULL, "
                    + "CAST(l.category AS text) FROM landmarks l LEFT JOIN destinations d ON d.id = l.destination_id "
                    + "WHERE l.id IN (:ids) AND l.is_active";
            case "car" -> "SELECT c.id, c.name, c.cover_image_url, c.destination_id, d.name, c.price_per_day, NULL, NULL, "
                    + "CAST(c.car_type AS text) FROM cars c LEFT JOIN destinations d ON d.id = c.destination_id "
                    + "WHERE c.id IN (:ids) AND c.is_active";
            default -> null;
        };
        if (sql == null) {
            items.clear();
            return;
        }
        List<UUID> ids = items.stream().map(RecommendationDTO::getEntityId).toList();
        Map<UUID, Object[]> info = new HashMap<>();
        for (Object[] r : (List<Object[]>) entityManager.createNativeQuery(sql).setParameter("ids", ids).getResultList()) {
            info.put((UUID) r[0], r);
        }
        Map<UUID, double[]> ratings = ratings(type, ids);
        items.removeIf(dto -> !info.containsKey(dto.getEntityId()));
        for (RecommendationDTO dto : items) {
            Object[] r = info.get(dto.getEntityId());
            dto.setName((String) r[1]);
            dto.setImageUrl((String) r[2]);
            dto.setDestinationId((UUID) r[3]);
            dto.setDestinationName((String) r[4]);
            dto.setPrice((BigDecimal) r[5]);
            dto.setStarRating(r[6] == null ? null : ((Number) r[6]).intValue());
            dto.setDurationDays(r[7] == null ? null : ((Number) r[7]).intValue());
            dto.setCategory((String) r[8]);
            double[] rating = ratings.get(dto.getEntityId());
            if (rating != null) {
                dto.setRating(Math.round(rating[0] * 10) / 10.0);
                dto.setReviewCount((long) rating[1]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, double[]> ratings(String type, List<UUID> ids) {
        Map<UUID, double[]> map = new HashMap<>();
        List<Object[]> rows = entityManager.createNativeQuery("SELECT entity_id, AVG(rating), COUNT(*) FROM posts "
                        + "WHERE entity_type = :t AND entity_id IN (:ids) AND status = 'approved' AND rating IS NOT NULL "
                        + "GROUP BY entity_id")
                .setParameter("t", type)
                .setParameter("ids", ids)
                .getResultList();
        for (Object[] r : rows) {
            map.put((UUID) r[0], new double[]{((Number) r[1]).doubleValue(), ((Number) r[2]).doubleValue()});
        }
        return map;
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
