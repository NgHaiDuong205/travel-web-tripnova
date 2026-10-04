package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ChatSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {

    @Query(value = "SELECT s FROM ChatSessionEntity s WHERE s.userId = :userId AND s.isActive = true "
            + "ORDER BY s.updatedAt DESC, s.id",
            countQuery = "SELECT COUNT(s) FROM ChatSessionEntity s WHERE s.userId = :userId AND s.isActive = true")
    Page<ChatSessionEntity> findActiveByUserId(@Param("userId") UUID userId, Pageable pageable);

    Optional<ChatSessionEntity> findByIdAndUserId(UUID id, UUID userId);

    /** Admin: lọc theo user, từ khoá (tiêu đề / email / tên), thời điểm cập nhật trong [from, to). */
    @Query(value = "SELECT s.* FROM chat_sessions s LEFT JOIN users u ON u.id = s.user_id " + ADMIN_WHERE
            + " ORDER BY s.updated_at DESC, s.id",
            countQuery = "SELECT COUNT(*) FROM chat_sessions s LEFT JOIN users u ON u.id = s.user_id " + ADMIN_WHERE,
            nativeQuery = true)
    Page<ChatSessionEntity> adminSearch(@Param("userId") UUID userId, @Param("q") String q,
                                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                        Pageable pageable);

    String ADMIN_WHERE = "WHERE (CAST(:userId AS uuid) IS NULL OR s.user_id = CAST(:userId AS uuid)) "
            + "AND (CAST(:q AS text) IS NULL OR s.title ILIKE '%' || CAST(:q AS text) || '%' "
            + "OR u.email ILIKE '%' || CAST(:q AS text) || '%' OR u.full_name ILIKE '%' || CAST(:q AS text) || '%') "
            + "AND (CAST(:from AS timestamp) IS NULL OR s.updated_at >= CAST(:from AS timestamp)) "
            + "AND (CAST(:to AS timestamp) IS NULL OR s.updated_at < CAST(:to AS timestamp))";
}
