package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AiSummarizeResultDTO;
import com.duong.travelweb.model.dto.AiSummaryDTO;
import com.duong.travelweb.model.entity.AiSummaryEntity;
import com.duong.travelweb.repository.AiSummaryRepository;
import com.duong.travelweb.service.AiJobService;
import com.duong.travelweb.service.AiSummaryService;
import com.duong.travelweb.service.EntityReferenceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AiSummaryServiceImpl implements AiSummaryService {
    private static final List<String> LANGUAGES = List.of("vi", "en");
    /** Số review tối đa gửi cho AI (review được bình chọn hữu ích nhiều trước, rồi mới nhất). */
    private static final int MAX_REVIEWS = 40;
    private static final int MAX_REVIEW_CHARS = 1500;

    private final AiSummaryRepository aiSummaryRepository;
    private final AiJobService aiJobService;
    private final EntityReferenceService entityReferenceService;
    private final ObjectMapper objectMapper;
    private final int minReviews;
    @PersistenceContext
    private EntityManager entityManager;

    public AiSummaryServiceImpl(AiSummaryRepository aiSummaryRepository, AiJobService aiJobService,
                                EntityReferenceService entityReferenceService, ObjectMapper objectMapper,
                                @Value("${app.ai.summaries.min-reviews:3}") int minReviews) {
        this.aiSummaryRepository = aiSummaryRepository;
        this.aiJobService = aiJobService;
        this.entityReferenceService = entityReferenceService;
        this.objectMapper = objectMapper;
        this.minReviews = Math.max(minReviews, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSummaryDTO getReviewSummary(String entityType, UUID entityId, String language) {
        String type = requireType(entityType);
        String lang = normalizeLanguage(language);
        long count = countApproved(type, entityId);
        if (count < minReviews) {
            return null;
        }
        return aiSummaryRepository.findOne(type, entityId, SUMMARY_TYPE_REVIEWS, lang)
                .map(summary -> toDTO(summary, count))
                .orElse(null);
    }

    @Override
    @Transactional
    public AiSummarizeResultDTO requestReviewSummary(String entityType, UUID entityId, String language) {
        String type = requireType(entityType);
        entityReferenceService.requireActiveName(type, entityId);
        AiSummarizeResultDTO result = new AiSummarizeResultDTO();
        long count = countApproved(type, entityId);
        if (count < minReviews) {
            result.setStatus("not_enough_reviews");
            return result;
        }
        AiSummaryDTO current = aiSummaryRepository.findOne(type, entityId, SUMMARY_TYPE_REVIEWS, normalizeLanguage(language))
                .map(summary -> toDTO(summary, count))
                .orElse(null);
        result.setSummary(current);
        if (current != null && !Boolean.TRUE.equals(current.getStale())) {
            result.setStatus("fresh");
            return result;
        }
        result.setStatus("queued");
        result.setJobId(aiJobService.enqueue(JOB_SUMMARIZE_REVIEWS, payload(type, entityId)));
        return result;
    }

    @Override
    @Transactional
    public void reviewsChanged(String entityType, UUID entityId) {
        if (entityType != null && entityId != null && EntityReferenceService.REVIEWABLE_TYPES.contains(entityType)) {
            aiJobService.enqueue(JOB_SUMMARIZE_REVIEWS, payload(entityType, entityId));
        }
    }

    @Override
    @Transactional
    public UUID regenerate(String entityType, UUID entityId) {
        String type = requireType(entityType);
        if (entityId == null) {
            throw ApiException.badRequest("Thiếu entityId");
        }
        if (countApproved(type, entityId) < minReviews) {
            throw ApiException.badRequest("Cần ít nhất " + minReviews + " review đã duyệt để tóm tắt");
        }
        return aiJobService.enqueue(JOB_SUMMARIZE_REVIEWS, payload(type, entityId));
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public int enqueueStale(int limit) {
        // Thiếu / cũ (số review đổi) / không còn đủ review; bỏ qua đối tượng đang có job hoặc vừa lỗi trong 24h.
        List<Object[]> rows = entityManager.createNativeQuery("WITH c AS (SELECT entity_type, entity_id, COUNT(*) AS n "
                        + "FROM posts WHERE status = 'approved' GROUP BY entity_type, entity_id), "
                        + "s AS (SELECT entity_type, entity_id, based_on_count FROM ai_summaries "
                        + "WHERE summary_type = :st AND language = 'vi'), "
                        + "x AS (SELECT c.entity_type, c.entity_id FROM c LEFT JOIN s USING (entity_type, entity_id) "
                        + "WHERE c.n >= :min AND (s.entity_id IS NULL OR s.based_on_count IS DISTINCT FROM c.n) "
                        + "UNION SELECT s.entity_type, s.entity_id FROM s LEFT JOIN c USING (entity_type, entity_id) "
                        + "WHERE COALESCE(c.n, 0) < :min) "
                        + "SELECT x.entity_type, CAST(x.entity_id AS text) FROM x WHERE NOT EXISTS (SELECT 1 FROM ai_jobs j "
                        + "WHERE j.job_type = :job AND j.payload ->> 'entityType' = x.entity_type "
                        + "AND j.payload ->> 'entityId' = CAST(x.entity_id AS text) AND (j.status IN ('pending', 'running') "
                        + "OR (j.status = 'failed' AND j.finished_at > now() - interval '1 day'))) LIMIT :limit")
                .setParameter("st", SUMMARY_TYPE_REVIEWS)
                .setParameter("min", minReviews)
                .setParameter("job", JOB_SUMMARIZE_REVIEWS)
                .setParameter("limit", Math.max(limit, 1))
                .getResultList();
        for (Object[] row : rows) {
            aiJobService.enqueue(JOB_SUMMARIZE_REVIEWS, payload((String) row[0], UUID.fromString((String) row[1])));
        }
        return rows.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiSummaryDTO> search(String entityType, String language, int page, int limit) {
        String type = entityType == null || entityType.isBlank() ? null : requireType(entityType);
        String lang = language == null || language.isBlank() ? null : normalizeLanguage(language);
        Page<AiSummaryEntity> result = aiSummaryRepository.search(SUMMARY_TYPE_REVIEWS, type, lang,
                PageRequest.of(Math.max(page, 1) - 1, limit));
        List<AiSummaryEntity> summaries = result.getContent();
        Map<String, Long> counts = new HashMap<>();
        Map<String, Set<UUID>> idsByType = new HashMap<>();
        if (!summaries.isEmpty()) {
            Set<UUID> ids = new HashSet<>();
            summaries.forEach(s -> {
                ids.add(s.getEntityId());
                idsByType.computeIfAbsent(s.getEntityType(), k -> new HashSet<>()).add(s.getEntityId());
            });
            for (Object[] row : aiSummaryRepository.countApprovedReviews(ids)) {
                counts.put(row[0] + ":" + row[1], ((Number) row[2]).longValue());
            }
        }
        Map<String, Map<UUID, String>> names = entityReferenceService.resolveNames(idsByType);
        List<AiSummaryDTO> dtos = new ArrayList<>();
        for (AiSummaryEntity summary : summaries) {
            AiSummaryDTO dto = toDTO(summary, counts.getOrDefault(summary.getEntityType() + ":" + summary.getEntityId(), 0L));
            dto.setEntityName(names.getOrDefault(summary.getEntityType(), Map.of()).get(summary.getEntityId()));
            dtos.add(dto);
        }
        return new PageImpl<>(dtos, result.getPageable(), result.getTotalElements());
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public ReviewRequest buildReviewRequest(String entityType, UUID entityId) {
        Object[] stats = (Object[]) entityManager.createNativeQuery("SELECT COUNT(*), AVG(rating) FROM posts "
                        + "WHERE entity_type = :t AND entity_id = :e AND status = 'approved'")
                .setParameter("t", entityType)
                .setParameter("e", entityId)
                .getSingleResult();
        int count = ((Number) stats[0]).intValue();
        String name = count < minReviews ? null : entityReferenceService
                .resolveNames(Map.of(entityType, List.of(entityId)))
                .getOrDefault(entityType, Map.of()).get(entityId);
        if (name == null) {
            aiSummaryRepository.deleteFor(entityType, entityId, SUMMARY_TYPE_REVIEWS);
            return null;
        }
        List<Object[]> rows = entityManager.createNativeQuery("SELECT rating, title, content, is_verified_booking, "
                        + "to_char(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD') FROM posts "
                        + "WHERE entity_type = :t AND entity_id = :e AND status = 'approved' "
                        + "ORDER BY (upvotes - downvotes >= 3) DESC, created_at DESC, id LIMIT :n")
                .setParameter("t", entityType)
                .setParameter("e", entityId)
                .setParameter("n", MAX_REVIEWS)
                .getResultList();
        List<Map<String, Object>> reviews = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> review = new LinkedHashMap<>();
            review.put("rating", row[0] == null ? null : ((Number) row[0]).intValue());
            review.put("title", row[1]);
            review.put("content", truncate((String) row[2], MAX_REVIEW_CHARS));
            review.put("verified", Boolean.TRUE.equals(row[3]));
            review.put("date", row[4]);
            reviews.add(review);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("entityType", entityType);
        body.put("entityName", truncate(name, 300));
        body.put("averageRating", stats[1] == null ? null : Math.round(((Number) stats[1]).doubleValue() * 10) / 10.0);
        body.put("reviewCount", count);
        body.put("reviews", reviews);
        return new ReviewRequest(body, count);
    }

    @Override
    @Transactional
    public void saveReviewSummaries(String entityType, UUID entityId, int basedOnCount, JsonNode aiResult) {
        JsonNode summaries = aiResult == null ? null : aiResult.get("summaries");
        if (summaries == null || !summaries.isObject()) {
            throw new IllegalStateException("Dịch vụ AI trả thiếu summaries");
        }
        String model = aiResult.hasNonNull("model") ? truncate(aiResult.get("model").asString(), 30) : null;
        for (String lang : LANGUAGES) {
            JsonNode part = summaries.get(lang);
            if (part == null || !part.hasNonNull("overview")) {
                throw new IllegalStateException("Dịch vụ AI trả thiếu bản " + lang);
            }
            aiSummaryRepository.upsert(entityType, entityId, SUMMARY_TYPE_REVIEWS, lang,
                    objectMapper.writeValueAsString(part), basedOnCount, model);
        }
    }

    private long countApproved(String entityType, UUID entityId) {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM posts "
                        + "WHERE entity_type = :t AND entity_id = :e AND status = 'approved'")
                .setParameter("t", entityType)
                .setParameter("e", entityId)
                .getSingleResult()).longValue();
    }

    private AiSummaryDTO toDTO(AiSummaryEntity summary, long currentCount) {
        AiSummaryDTO dto = new AiSummaryDTO();
        dto.setEntityType(summary.getEntityType());
        dto.setEntityId(summary.getEntityId());
        dto.setSummaryType(summary.getSummaryType());
        dto.setLanguage(summary.getLanguage() == null ? null : summary.getLanguage().trim());
        JsonNode content = objectMapper.readTree(summary.getContent());
        dto.setOverview(content.hasNonNull("overview") ? content.get("overview").asString() : null);
        dto.setPros(readList(content, "pros"));
        dto.setCons(readList(content, "cons"));
        dto.setBestFor(readList(content, "bestFor"));
        dto.setBasedOnCount(summary.getBasedOnCount());
        dto.setCurrentReviewCount(currentCount);
        dto.setStale(summary.getBasedOnCount() == null || summary.getBasedOnCount() != currentCount);
        dto.setModelVersion(summary.getModelVersion());
        dto.setGeneratedAt(summary.getGeneratedAt());
        return dto;
    }

    private static List<String> readList(JsonNode content, String field) {
        List<String> items = new ArrayList<>();
        JsonNode node = content.get(field);
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (item.isString()) {
                    items.add(item.asString());
                }
            });
        }
        return items;
    }

    private static Map<String, Object> payload(String entityType, UUID entityId) {
        // Đúng 2 khoá: so trùng job pending / lỗi gần đây theo payload.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entityType", entityType);
        payload.put("entityId", entityId.toString());
        return payload;
    }

    private static String requireType(String entityType) {
        String type = entityType == null ? null : entityType.trim().toLowerCase();
        if (!EntityReferenceService.REVIEWABLE_TYPES.contains(type)) {
            throw ApiException.badRequest("entityType phải là một trong: "
                    + String.join(", ", EntityReferenceService.REVIEWABLE_TYPES));
        }
        return type;
    }

    private static String normalizeLanguage(String language) {
        String lang = language == null || language.isBlank() ? "vi" : language.trim().toLowerCase();
        if (!LANGUAGES.contains(lang)) {
            throw ApiException.badRequest("lang phải là vi hoặc en");
        }
        return lang;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
