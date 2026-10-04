package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    @Query("SELECT m FROM ChatMessageEntity m WHERE m.sessionId = :sessionId ORDER BY m.createdAt, m.id")
    List<ChatMessageEntity> findBySessionId(@Param("sessionId") UUID sessionId);

    /** N tin gần nhất (mới nhất trước) — dùng làm lịch sử hội thoại gửi cho AI. */
    @Query(value = "SELECT * FROM chat_messages WHERE session_id = :sessionId "
            + "ORDER BY created_at DESC, id DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessageEntity> findRecent(@Param("sessionId") UUID sessionId, @Param("limit") int limit);

    /** Số câu hỏi user đã gửi từ thời điểm `since` (giới hạn tần suất, tính trên mọi phiên). */
    @Query("SELECT COUNT(m) FROM ChatMessageEntity m, ChatSessionEntity s "
            + "WHERE s.id = m.sessionId AND s.userId = :userId AND m.role = 'user' AND m.createdAt > :since")
    long countUserQuestionsSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT m.sessionId, COUNT(m) FROM ChatMessageEntity m WHERE m.sessionId IN :sessionIds GROUP BY m.sessionId")
    List<Object[]> countBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);
}
