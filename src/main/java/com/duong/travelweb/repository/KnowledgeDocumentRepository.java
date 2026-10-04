package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.KnowledgeDocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Đọc kho tài liệu RAG (dịch vụ Python ghi; Spring chỉ đọc danh sách cho trang admin). */
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, UUID> {

    @Query(value = "SELECT d.* FROM knowledge_documents d " + SEARCH_WHERE + " ORDER BY d.updated_at DESC NULLS LAST, d.id",
            countQuery = "SELECT COUNT(*) FROM knowledge_documents d " + SEARCH_WHERE,
            nativeQuery = true)
    Page<KnowledgeDocumentEntity> search(@Param("q") String q, @Param("sourceType") String sourceType,
                                         @Param("language") String language, @Param("active") Boolean active,
                                         Pageable pageable);

    @Query(value = "SELECT document_id, COUNT(*) FROM knowledge_chunks WHERE document_id IN (:ids) GROUP BY document_id",
            nativeQuery = true)
    List<Object[]> countChunks(@Param("ids") Collection<UUID> ids);

    String SEARCH_WHERE = "WHERE (CAST(:q AS text) IS NULL OR d.title ILIKE '%' || CAST(:q AS text) || '%') "
            + "AND (CAST(:sourceType AS text) IS NULL OR d.source_type = CAST(:sourceType AS text)) "
            + "AND (CAST(:language AS text) IS NULL OR d.language = CAST(:language AS bpchar)) "
            + "AND (CAST(:active AS boolean) IS NULL OR d.is_active = CAST(:active AS boolean))";
}
