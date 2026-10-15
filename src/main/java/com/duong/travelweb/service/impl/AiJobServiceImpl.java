package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AiJobDTO;
import com.duong.travelweb.model.entity.AiJobEntity;
import com.duong.travelweb.repository.AiJobRepository;
import com.duong.travelweb.service.AiJobService;
import com.duong.travelweb.service.EntityReferenceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AiJobServiceImpl implements AiJobService {
    private static final int MAX_ERROR_LENGTH = 2000;

    private final AiJobRepository aiJobRepository;
    private final EntityReferenceService entityReferenceService;
    private final ObjectMapper objectMapper;
    @PersistenceContext
    private EntityManager entityManager;

    public AiJobServiceImpl(AiJobRepository aiJobRepository, EntityReferenceService entityReferenceService,
                            ObjectMapper objectMapper) {
        this.aiJobRepository = aiJobRepository;
        this.entityReferenceService = entityReferenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UUID enqueue(String jobType, Map<String, Object> payload) {
        String json = objectMapper.writeValueAsString(payload);
        // Khoá theo (loại, payload) tới hết transaction: 2 request đồng thời không cùng chèn job trùng.
        entityManager.createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(CAST(:k AS text)))) x")
                .setParameter("k", jobType + ":" + json)
                .getResultList();
        List<?> existing = entityManager.createNativeQuery("SELECT CAST(id AS text) FROM ai_jobs "
                        + "WHERE status = 'pending' AND job_type = :t AND payload = CAST(:p AS jsonb) LIMIT 1")
                .setParameter("t", jobType)
                .setParameter("p", json)
                .getResultList();
        if (!existing.isEmpty()) {
            return UUID.fromString((String) existing.get(0));
        }
        Object id = entityManager.createNativeQuery("INSERT INTO ai_jobs (job_type, payload, status, attempts, created_at) "
                        + "VALUES (:t, CAST(:p AS jsonb), 'pending', 0, now()) RETURNING CAST(id AS text)")
                .setParameter("t", jobType)
                .setParameter("p", json)
                .getSingleResult();
        return UUID.fromString((String) id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiJobDTO> search(String status, String jobType, UUID entityId, int page, int limit) {
        String normalizedStatus = blankToNull(status);
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) {
            throw ApiException.badRequest("status phải là một trong: " + String.join(", ", STATUSES));
        }
        Page<AiJobEntity> result = aiJobRepository.search(normalizedStatus, blankToNull(jobType),
                entityId == null ? null : entityId.toString(), PageRequest.of(Math.max(page, 1) - 1, limit));
        return new PageImpl<>(toDTOs(result.getContent()), result.getPageable(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AiJobDTO get(UUID id) {
        return toDTOs(List.of(findJob(id))).get(0);
    }

    @Override
    @Transactional
    public AiJobDTO retry(UUID id) {
        AiJobEntity job = findJob(id);
        if (!"failed".equals(job.getStatus()) && !"cancelled".equals(job.getStatus())) {
            throw ApiException.conflict("Chỉ chạy lại được job failed hoặc cancelled");
        }
        job.setStatus("pending");
        job.setAttempts((short) 0);
        job.setErrorMessage(null);
        job.setStartedAt(null);
        job.setFinishedAt(null);
        return toDTOs(List.of(job)).get(0);
    }

    @Override
    @Transactional
    public AiJobDTO cancel(UUID id) {
        AiJobEntity job = findJob(id);
        if (!"pending".equals(job.getStatus())) {
            throw ApiException.conflict("Chỉ huỷ được job đang chờ (pending)");
        }
        job.setStatus("cancelled");
        job.setFinishedAt(LocalDateTime.now());
        return toDTOs(List.of(job)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        STATUSES.forEach(s -> counts.put(s, 0L));
        for (Object[] row : aiJobRepository.countByStatus()) {
            if (row[0] != null) {
                counts.put((String) row[0], ((Number) row[1]).longValue());
            }
        }
        return counts;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public AiJobEntity claimNext(Collection<String> jobTypes, int retryBackoffMinutes) {
        if (jobTypes.isEmpty()) {
            return null;
        }
        // Lần thử thứ n (n ≥ 1 lượt đã dùng) chờ n × backoff phút kể từ lần chạy trước.
        List<AiJobEntity> rows = entityManager.createNativeQuery("UPDATE ai_jobs SET status = 'running', "
                        + "started_at = now(), finished_at = NULL, attempts = COALESCE(attempts, 0) + 1 "
                        + "WHERE id = (SELECT id FROM ai_jobs WHERE status = 'pending' AND job_type IN (:types) "
                        + "AND (COALESCE(attempts, 0) = 0 OR started_at IS NULL "
                        + "OR started_at < now() - make_interval(mins => :backoff * attempts)) "
                        + "ORDER BY created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED) RETURNING *", AiJobEntity.class)
                .setParameter("types", jobTypes)
                .setParameter("backoff", retryBackoffMinutes)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    @Transactional
    public void complete(UUID id) {
        entityManager.createNativeQuery("UPDATE ai_jobs SET status = 'done', finished_at = now(), error_message = NULL "
                        + "WHERE id = :id AND status = 'running'")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void fail(UUID id, String error, boolean finalFailure) {
        entityManager.createNativeQuery("UPDATE ai_jobs SET status = :s, error_message = :e, "
                        + "finished_at = CASE WHEN :s = 'failed' THEN now() ELSE NULL END "
                        + "WHERE id = :id AND status = 'running'")
                .setParameter("s", finalFailure ? "failed" : "pending")
                .setParameter("e", truncate(error))
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void release(UUID id, String note) {
        entityManager.createNativeQuery("UPDATE ai_jobs SET status = 'pending', error_message = :e, "
                        + "attempts = GREATEST(COALESCE(attempts, 1) - 1, 0) WHERE id = :id AND status = 'running'")
                .setParameter("e", truncate(note))
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int recoverStuck(int staleMinutes, int maxAttempts) {
        return entityManager.createNativeQuery("UPDATE ai_jobs SET "
                        + "status = CASE WHEN COALESCE(attempts, 0) >= :max THEN 'failed' ELSE 'pending' END, "
                        + "finished_at = CASE WHEN COALESCE(attempts, 0) >= :max THEN now() ELSE NULL END, "
                        + "error_message = 'Bị dừng khi đang chạy (máy chủ khởi động lại?)' "
                        + "WHERE status = 'running' AND started_at < now() - make_interval(mins => :mins)")
                .setParameter("max", maxAttempts)
                .setParameter("mins", staleMinutes)
                .executeUpdate();
    }

    private AiJobEntity findJob(UUID id) {
        return aiJobRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy job"));
    }

    /** Payload có entityType + entityId (job theo đối tượng) → kèm tên đối tượng, tra theo lô. */
    private List<AiJobDTO> toDTOs(List<AiJobEntity> jobs) {
        List<AiJobDTO> dtos = new ArrayList<>();
        Map<String, Set<UUID>> idsByType = new HashMap<>();
        for (AiJobEntity job : jobs) {
            AiJobDTO dto = new AiJobDTO();
            dto.setId(job.getId());
            dto.setJobType(job.getJobType());
            dto.setStatus(job.getStatus());
            dto.setAttempts(job.getAttempts() == null ? 0 : job.getAttempts().intValue());
            dto.setErrorMessage(job.getErrorMessage());
            dto.setStartedAt(job.getStartedAt());
            dto.setFinishedAt(job.getFinishedAt());
            dto.setCreatedAt(job.getCreatedAt());
            JsonNode payload = job.getPayload() == null ? null : objectMapper.readTree(job.getPayload());
            dto.setPayload(payload);
            if (payload != null && payload.hasNonNull("entityType") && payload.hasNonNull("entityId")) {
                try {
                    dto.setEntityType(payload.get("entityType").asString());
                    dto.setEntityId(UUID.fromString(payload.get("entityId").asString()));
                    idsByType.computeIfAbsent(dto.getEntityType(), k -> new HashSet<>()).add(dto.getEntityId());
                } catch (IllegalArgumentException ignored) {
                    dto.setEntityId(null);
                }
            }
            dtos.add(dto);
        }
        Map<String, Map<UUID, String>> names = entityReferenceService.resolveNames(idsByType);
        for (AiJobDTO dto : dtos) {
            if (dto.getEntityId() != null) {
                dto.setEntityName(names.getOrDefault(dto.getEntityType(), Map.of()).get(dto.getEntityId()));
            }
        }
        return dtos;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
