package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.SearchQueryDTO;
import com.duong.travelweb.model.dto.SearchQueryStatDTO;
import com.duong.travelweb.model.dto.SearchResponseDTO;
import com.duong.travelweb.model.dto.SearchResultDTO;
import com.duong.travelweb.model.entity.SearchQueryEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.SearchQueryRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.SearchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_TOKENS = 6;

    /**
     * Mỗi loại: FROM/JOIN, chuỗi để khớp từ khoá (haystack), biểu thức tên để xếp hạng, và các cột hiển thị.
     * Kết quả mỗi dòng: id, title, subtitle, image, price, score, total (COUNT(*) OVER()).
     */
    private record TypeSpec(String from, String where, String haystack, String rankExpr, String title, String subtitle,
                            String image, String price, String priceUnit, String orderTail) {
    }

    private static final Map<String, TypeSpec> SPECS = new LinkedHashMap<>();

    static {
        SPECS.put("destination", new TypeSpec(
                "destinations d JOIN countries c ON c.id = d.country_id",
                "d.is_active = true",
                "d.name || ' ' || c.name",
                "d.name",
                "d.name", "c.name", "d.cover_image_url", "NULL", null,
                "d.is_popular DESC, d.name"));
        SPECS.put("hotel", new TypeSpec(
                "hotels h JOIN destinations d ON d.id = h.destination_id JOIN countries c ON c.id = d.country_id",
                "h.is_active = true",
                "h.name || ' ' || COALESCE(h.address, '') || ' ' || d.name || ' ' || c.name",
                "h.name",
                "h.name", "d.name || ', ' || c.name || ' · ' || h.star_rating || '★'", "h.cover_image_url",
                "(SELECT MIN(rt.price_per_night) FROM room_types rt WHERE rt.hotel_id = h.id)", "night",
                "h.star_rating DESC, h.name"));
        SPECS.put("tour", new TypeSpec(
                "tours t LEFT JOIN destinations d ON d.id = t.destination_id",
                "t.is_active = true AND (t.departure_date IS NULL OR t.departure_date >= CURRENT_DATE)",
                "t.name || ' ' || COALESCE(d.name, '') || ' ' || COALESCE(t.departure_location, '')",
                "t.name",
                "t.name", "COALESCE(d.name || ' · ', '') || t.duration_days || ' days'", "t.cover_image_url",
                "t.price_adult", "person",
                "t.name"));
        SPECS.put("car", new TypeSpec(
                "cars car LEFT JOIN destinations d ON d.id = car.destination_id",
                "car.is_active = true",
                "COALESCE(car.name, '') || ' ' || COALESCE(car.brand, '') || ' ' || COALESCE(car.model, '') || ' ' "
                        + "|| CAST(car.car_type AS text) || ' ' || COALESCE(d.name, '')",
                "COALESCE(car.name, car.brand || ' ' || car.model)",
                "COALESCE(car.name, car.brand || ' ' || car.model)",
                "CAST(car.car_type AS text) || ' · ' || car.seats || ' seats' || COALESCE(' · ' || d.name, '')",
                "car.cover_image_url", "car.price_per_day", "day",
                "car.price_per_day"));
        SPECS.put("flight", new TypeSpec(
                "flights f",
                "f.is_active = true AND f.departure_time > now() + interval '2 hours'",
                "f.airline || ' ' || f.flight_number || ' ' || f.departure_city || ' ' || f.departure_airport_code || ' '"
                        + " || f.arrival_city || ' ' || f.arrival_airport_code",
                "f.departure_city || ' ' || f.arrival_city",
                "f.airline || ' ' || f.flight_number",
                "f.departure_city || ' (' || f.departure_airport_code || ') → ' || f.arrival_city || ' ('"
                        + " || f.arrival_airport_code || ') · ' || to_char(f.departure_time, 'DD Mon HH24:MI')",
                "f.airline_logo_url", "f.base_price", "person",
                "f.departure_time"));
    }

    private final SearchQueryRepository searchQueryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    /** Có extension unaccent không (kiểm tra 1 lần): có thì tìm "ha noi" ra "Hà Nội". */
    private volatile Boolean unaccent;

    @PersistenceContext
    private EntityManager entityManager;

    public SearchServiceImpl(SearchQueryRepository searchQueryRepository, UserRepository userRepository,
                             ObjectMapper objectMapper) {
        this.searchQueryRepository = searchQueryRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SearchResponseDTO search(String q, String type, int page, int limit, UUID userId) {
        String query = normalizeQuery(q);
        String kind = type == null || type.isBlank() ? "all" : type.trim().toLowerCase();
        if (!"all".equals(kind) && !SPECS.containsKey(kind)) {
            throw ApiException.badRequest("type phải là all hoặc một trong: " + String.join(", ", TYPES));
        }
        List<String> tokens = tokens(query);
        Map<String, Long> counts = new LinkedHashMap<>();
        List<SearchResultDTO> results = new ArrayList<>();
        for (String key : "all".equals(kind) ? TYPES : List.of(kind)) {
            int offset = "all".equals(kind) ? 0 : (Math.max(page, 1) - 1) * limit;
            long total = runQuery(key, SPECS.get(key), query, tokens, limit, offset, results);
            counts.put(key, total);
        }

        SearchResponseDTO response = new SearchResponseDTO();
        response.setQuery(query);
        response.setType(kind);
        response.setCounts(counts);
        response.setResults(results);
        if (page <= 1) {
            SearchQueryEntity log = new SearchQueryEntity();
            log.setUserId(userId);
            log.setQueryText(query);
            log.setFilters(objectMapper.writeValueAsString(Map.of("type", kind)));
            log.setResultCount((int) Math.min(Integer.MAX_VALUE, counts.values().stream().mapToLong(Long::longValue).sum()));
            response.setSearchId(searchQueryRepository.save(log).getId());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchResultDTO> suggest(String q, int limit) {
        String query = q == null ? "" : q.trim().replaceAll("\\s+", " ");
        if (query.length() < 2) {
            return List.of();
        }
        query = query.length() > MAX_QUERY_LENGTH ? query.substring(0, MAX_QUERY_LENGTH) : query;
        List<String> tokens = tokens(query);
        List<SearchResultDTO> all = new ArrayList<>();
        for (String key : List.of("destination", "hotel", "tour")) {
            runQuery(key, SPECS.get(key), query, tokens, limit, 0, all);
        }
        all.addAll(suggestCities(query, tokens, limit));
        return all.stream()
                .sorted(Comparator.comparing(SearchResultDTO::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional
    public void recordClick(Long searchId, String entityType, UUID entityId, UUID userId) {
        String kind = entityType == null ? "" : entityType.trim().toLowerCase();
        if (searchId == null || entityId == null || !SPECS.containsKey(kind)) {
            throw ApiException.badRequest("Thiếu searchId / entityType / entityId hợp lệ");
        }
        SearchQueryEntity log = searchQueryRepository.findById(searchId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lượt tìm kiếm"));
        // Lượt tìm của user khác -> bỏ qua lặng lẽ (không lộ thông tin), lượt ẩn danh thì ai bấm cũng được ghi.
        if (log.getUserId() != null && !log.getUserId().equals(userId)) {
            return;
        }
        if (log.getClickedEntityId() == null) {
            log.setClickedEntityType(kind);
            log.setClickedEntityId(entityId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SearchQueryDTO> findQueries(UUID userId, String q, LocalDate from, LocalDate to, int page, int limit) {
        Specification<SearchQueryEntity> spec = (root, query, cb) -> cb.conjunction();
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (q != null && !q.isBlank()) {
            String pattern = "%" + escapeLike(q.trim().toLowerCase()) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("queryText")), pattern, '!'));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
        }
        Page<SearchQueryEntity> result = searchQueryRepository.findAll(spec,
                PageRequest.of(Math.max(page, 1) - 1, limit, Sort.by(Sort.Order.desc("id"))));
        List<UUID> userIds = result.getContent().stream().map(SearchQueryEntity::getUserId).filter(id -> id != null)
                .distinct().toList();
        Map<UUID, String> emails = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
        return result.map(entity -> {
            SearchQueryDTO dto = new SearchQueryDTO();
            dto.setId(entity.getId());
            dto.setUserId(entity.getUserId());
            dto.setUserEmail(entity.getUserId() == null ? null : emails.get(entity.getUserId()));
            dto.setQueryText(entity.getQueryText());
            dto.setFilters(entity.getFilters());
            dto.setResultCount(entity.getResultCount());
            dto.setClickedEntityType(entity.getClickedEntityType());
            dto.setClickedEntityId(entity.getClickedEntityId());
            dto.setCreatedAt(entity.getCreatedAt());
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchQueryStatDTO> topQueries(LocalDate from, LocalDate to, int limit) {
        LocalDateTime start = (from != null ? from : LocalDate.now().minusDays(29)).atStartOfDay();
        LocalDateTime end = (to != null ? to : LocalDate.now()).plusDays(1).atStartOfDay();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT lower(query_text) AS q, COUNT(*), COUNT(DISTINCT user_id),
                               COUNT(clicked_entity_id), COUNT(*) FILTER (WHERE result_count = 0)
                        FROM search_queries
                        WHERE created_at >= :start AND created_at < :end
                        GROUP BY lower(query_text)
                        ORDER BY 2 DESC, 1
                        LIMIT :limit
                        """)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream().map(r -> {
            SearchQueryStatDTO dto = new SearchQueryStatDTO();
            dto.setQueryText((String) r[0]);
            dto.setSearches(((Number) r[1]).longValue());
            dto.setUsers(((Number) r[2]).longValue());
            dto.setClicks(((Number) r[3]).longValue());
            dto.setZeroResults(((Number) r[4]).longValue());
            return dto;
        }).toList();
    }

    // ================= SQL =================

    /** Thêm kết quả vào out, trả về tổng số dòng khớp. */
    private long runQuery(String type, TypeSpec spec, String query, List<String> tokens, int limit, int offset,
                          List<SearchResultDTO> out) {
        Function<String, String> n = this::norm;
        StringBuilder sql = new StringBuilder("SELECT CAST(")
                .append(spec.from().split(" ")[1]).append(".id AS text), ") // alias bảng chính, VD "hotels h" -> h
                .append(spec.title()).append(", ")
                .append(spec.subtitle()).append(", ")
                .append(spec.image()).append(", ")
                .append(spec.price()).append(", ")
                .append("similarity(").append(n.apply(spec.rankExpr())).append(", ").append(n.apply("CAST(:q AS text)")).append(")")
                .append(" + CASE WHEN ").append(n.apply(spec.rankExpr())).append(" LIKE ")
                .append(n.apply("CAST(:prefix AS text)")).append(" ESCAPE '!' THEN 0.5 ELSE 0 END AS score, ")
                .append("COUNT(*) OVER () AS total FROM ").append(spec.from())
                .append(" WHERE ").append(spec.where());
        for (int i = 0; i < tokens.size(); i++) {
            sql.append(" AND ").append(n.apply(spec.haystack())).append(" LIKE ")
                    .append(n.apply("CAST(:t" + i + " AS text)")).append(" ESCAPE '!'");
        }
        sql.append(" ORDER BY score DESC, ").append(spec.orderTail()).append(" LIMIT :limit OFFSET :offset");

        Query nativeQuery = entityManager.createNativeQuery(sql.toString())
                .setParameter("q", query.toLowerCase())
                .setParameter("prefix", escapeLike(query.toLowerCase()) + "%")
                .setParameter("limit", limit)
                .setParameter("offset", offset);
        for (int i = 0; i < tokens.size(); i++) {
            nativeQuery.setParameter("t" + i, "%" + escapeLike(tokens.get(i)) + "%");
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();
        long total = 0;
        for (Object[] r : rows) {
            SearchResultDTO dto = new SearchResultDTO();
            dto.setType(type);
            dto.setId(UUID.fromString((String) r[0]));
            dto.setTitle((String) r[1]);
            dto.setSubtitle((String) r[2]);
            dto.setImageUrl((String) r[3]);
            dto.setPrice(r[4] == null ? null : new BigDecimal(r[4].toString()));
            dto.setPriceUnit(r[4] == null ? null : spec.priceUnit());
            dto.setScore(r[5] == null ? 0 : ((Number) r[5]).doubleValue());
            total = ((Number) r[6]).longValue();
            out.add(dto);
        }
        if (rows.isEmpty() && offset > 0) {
            // Trang vượt quá: vẫn cần tổng để FE phân trang.
            total = countOnly(spec, tokens);
        }
        return total;
    }

    private long countOnly(TypeSpec spec, List<String> tokens) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(spec.from()).append(" WHERE ").append(spec.where());
        for (int i = 0; i < tokens.size(); i++) {
            sql.append(" AND ").append(norm(spec.haystack())).append(" LIKE ").append(norm("CAST(:t" + i + " AS text)"))
                    .append(" ESCAPE '!'");
        }
        Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < tokens.size(); i++) {
            query.setParameter("t" + i, "%" + escapeLike(tokens.get(i)) + "%");
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    /** Thành phố có chuyến bay đang bán (đi hoặc đến) — gợi ý cho ô tìm chuyến bay. */
    private List<SearchResultDTO> suggestCities(String query, List<String> tokens, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT city, code, MAX(score) FROM (
                    SELECT departure_city AS city, departure_airport_code AS code FROM flights
                    WHERE is_active = true AND departure_time > now()
                    UNION
                    SELECT arrival_city, arrival_airport_code FROM flights
                    WHERE is_active = true AND departure_time > now()
                ) cities CROSS JOIN LATERAL (SELECT similarity(""")
                .append(norm("city")).append(", ").append(norm("CAST(:q AS text)")).append(") AS score) s WHERE 1 = 1");
        for (int i = 0; i < tokens.size(); i++) {
            sql.append(" AND ").append(norm("city || ' ' || code")).append(" LIKE ").append(norm("CAST(:t" + i + " AS text)"))
                    .append(" ESCAPE '!'");
        }
        sql.append(" GROUP BY city, code ORDER BY 3 DESC, city LIMIT :limit");
        Query nativeQuery = entityManager.createNativeQuery(sql.toString())
                .setParameter("q", query.toLowerCase())
                .setParameter("limit", limit);
        for (int i = 0; i < tokens.size(); i++) {
            nativeQuery.setParameter("t" + i, "%" + escapeLike(tokens.get(i)) + "%");
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();
        List<SearchResultDTO> result = new ArrayList<>();
        for (Object[] r : rows) {
            SearchResultDTO dto = new SearchResultDTO();
            dto.setType("city");
            dto.setTitle((String) r[0]);
            dto.setSubtitle((String) r[1]);
            dto.setScore(r[2] == null ? 0 : ((Number) r[2]).doubleValue());
            result.add(dto);
        }
        return result;
    }

    private String norm(String expr) {
        if (unaccent == null) {
            unaccent = (Boolean) entityManager
                    .createNativeQuery("SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'unaccent')")
                    .getSingleResult();
        }
        return unaccent ? "lower(unaccent(" + expr + "))" : "lower(" + expr + ")";
    }

    private static String normalizeQuery(String q) {
        String query = q == null ? "" : q.trim().replaceAll("\\s+", " ");
        if (query.isEmpty()) {
            throw ApiException.badRequest("Nhập từ khoá tìm kiếm");
        }
        return query.length() > MAX_QUERY_LENGTH ? query.substring(0, MAX_QUERY_LENGTH) : query;
    }

    /** Các từ khác nhau (tối đa 6), chữ thường; mọi từ đều phải xuất hiện. */
    private static List<String> tokens(String query) {
        return Arrays.stream(query.toLowerCase().split(" "))
                .filter(t -> !t.isBlank())
                .distinct()
                .limit(MAX_TOKENS)
                .toList();
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
