package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.AiSummaryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bản tóm tắt do AI sinh (ai_summaries), unique theo (entity_type, entity_id, summary_type, language). */
public interface AiSummaryRepository extends JpaRepository<AiSummaryEntity, UUID> {

    @Query(value = "SELECT * FROM ai_summaries WHERE entity_type = :t AND entity_id = :e AND summary_type = :s "
            + "AND language = CAST(:l AS bpchar)", nativeQuery = true)
    Optional<AiSummaryEntity> findOne(@Param("t") String entityType, @Param("e") UUID entityId,
                                      @Param("s") String summaryType, @Param("l") String language);

    @Modifying
    @Query(value = "INSERT INTO ai_summaries (entity_type, entity_id, summary_type, language, content, based_on_count, "
            + "model_version, generated_at) VALUES (:t, :e, :s, CAST(:l AS bpchar), :c, :n, :m, now()) "
            + "ON CONFLICT (entity_type, entity_id, summary_type, language) DO UPDATE SET content = EXCLUDED.content, "
            + "based_on_count = EXCLUDED.based_on_count, model_version = EXCLUDED.model_version, generated_at = now()",
            nativeQuery = true)
    int upsert(@Param("t") String entityType, @Param("e") UUID entityId, @Param("s") String summaryType,
               @Param("l") String language, @Param("c") String content, @Param("n") int basedOnCount,
               @Param("m") String modelVersion);

    @Modifying
    @Query(value = "DELETE FROM ai_summaries WHERE entity_type = :t AND entity_id = :e AND summary_type = :s",
            nativeQuery = true)
    int deleteFor(@Param("t") String entityType, @Param("e") UUID entityId, @Param("s") String summaryType);

    @Query(value = "SELECT s.* FROM ai_summaries s " + SEARCH_WHERE + " ORDER BY s.generated_at DESC, s.id",
            countQuery = "SELECT COUNT(*) FROM ai_summaries s " + SEARCH_WHERE,
            nativeQuery = true)
    Page<AiSummaryEntity> search(@Param("summaryType") String summaryType, @Param("entityType") String entityType,
                                 @Param("language") String language, Pageable pageable);

    /** Số review đã duyệt theo đối tượng: [entity_type, entity_id, count]. */
    @Query(value = "SELECT entity_type, entity_id, COUNT(*) FROM posts WHERE status = 'approved' "
            + "AND entity_id IN (:ids) GROUP BY entity_type, entity_id", nativeQuery = true)
    List<Object[]> countApprovedReviews(@Param("ids") Collection<UUID> entityIds);

    String SEARCH_WHERE = "WHERE s.summary_type = CAST(:summaryType AS text) "
            + "AND (CAST(:entityType AS text) IS NULL OR s.entity_type = CAST(:entityType AS text)) "
            + "AND (CAST(:language AS text) IS NULL OR s.language = CAST(:language AS bpchar))";
}
