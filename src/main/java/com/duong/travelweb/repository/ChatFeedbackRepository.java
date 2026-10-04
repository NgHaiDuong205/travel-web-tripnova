package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ChatFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedbackEntity, UUID> {

    /** Mỗi user 1 đánh giá cho mỗi tin nhắn; gửi lại thì ghi đè (unique index uq_chat_feedback_message_user). */
    @Modifying
    @Query(value = "INSERT INTO chat_feedback (message_id, user_id, rating, is_helpful, comment, created_at) "
            + "VALUES (:messageId, :userId, CAST(:rating AS smallint), CAST(:helpful AS boolean), CAST(:comment AS text), now()) "
            + "ON CONFLICT (message_id, user_id) DO UPDATE SET rating = EXCLUDED.rating, "
            + "is_helpful = EXCLUDED.is_helpful, comment = EXCLUDED.comment, created_at = now()", nativeQuery = true)
    int upsert(@Param("messageId") UUID messageId, @Param("userId") UUID userId, @Param("rating") Short rating,
               @Param("helpful") Boolean helpful, @Param("comment") String comment);

    @Query("SELECT f FROM ChatFeedbackEntity f WHERE f.userId = :userId AND f.messageId IN :messageIds")
    List<ChatFeedbackEntity> findByUserAndMessages(@Param("userId") UUID userId,
                                                   @Param("messageIds") Collection<UUID> messageIds);

    @Query("SELECT f FROM ChatFeedbackEntity f WHERE f.messageId IN :messageIds")
    List<ChatFeedbackEntity> findByMessages(@Param("messageIds") Collection<UUID> messageIds);
}
