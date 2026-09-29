package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.PostReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostReactionRepository extends JpaRepository<PostReactionEntity, UUID> {

    /** Mỗi user 1 reaction / bài (UNIQUE post_id, user_id): thêm mới hoặc đổi loại. */
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO post_reactions (post_id, user_id, reaction_type, created_at) " +
                   "VALUES (:postId, :userId, CAST(:type AS reaction_type), now()) " +
                   "ON CONFLICT (post_id, user_id) DO UPDATE SET reaction_type = EXCLUDED.reaction_type, created_at = now()",
           nativeQuery = true)
    void upsert(@Param("postId") UUID postId, @Param("userId") UUID userId, @Param("type") String type);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM post_reactions WHERE post_id = :postId AND user_id = :userId", nativeQuery = true)
    int deleteByPostAndUser(@Param("postId") UUID postId, @Param("userId") UUID userId);

    /** [postId, reaction_type] của user với các bài cho trước. */
    @Query("SELECT r.post.id, r.reactionType FROM PostReactionEntity r WHERE r.user.id = :userId AND r.post.id IN :postIds")
    List<Object[]> findUserReactions(@Param("userId") UUID userId, @Param("postIds") Collection<UUID> postIds);
}
