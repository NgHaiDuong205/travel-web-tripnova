package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.AiJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Hàng đợi job AI chạy nền (ai_jobs). Nhận job / khôi phục job kẹt nằm trong AiJobServiceImpl (UPDATE ... RETURNING). */
public interface AiJobRepository extends JpaRepository<AiJobEntity, UUID> {

    @Query(value = "SELECT j.* FROM ai_jobs j " + SEARCH_WHERE + " ORDER BY j.created_at DESC, j.id",
            countQuery = "SELECT COUNT(*) FROM ai_jobs j " + SEARCH_WHERE,
            nativeQuery = true)
    Page<AiJobEntity> search(@Param("status") String status, @Param("jobType") String jobType,
                             @Param("entityId") String entityId, Pageable pageable);

    @Query(value = "SELECT status, COUNT(*) FROM ai_jobs GROUP BY status", nativeQuery = true)
    List<Object[]> countByStatus();

    String SEARCH_WHERE = "WHERE (CAST(:status AS text) IS NULL OR j.status = CAST(:status AS text)) "
            + "AND (CAST(:jobType AS text) IS NULL OR j.job_type = CAST(:jobType AS text)) "
            + "AND (CAST(:entityId AS text) IS NULL OR j.payload ->> 'entityId' = CAST(:entityId AS text))";
}
