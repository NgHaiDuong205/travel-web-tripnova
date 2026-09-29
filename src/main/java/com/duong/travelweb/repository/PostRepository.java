package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.PostEntity;
import com.duong.travelweb.repository.custom.PostRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID>, PostRepositoryCustom {

    @Query("SELECT p FROM PostEntity p JOIN FETCH p.user WHERE p.id = :id")
    Optional<PostEntity> findWithUser(@Param("id") UUID id);

    /** [rating, số bài] của các bài đã duyệt có chấm điểm. */
    @Query("SELECT p.rating, COUNT(p) FROM PostEntity p WHERE p.entityType = :entityType AND p.entityId = :entityId " +
           "AND p.status = 'approved' AND p.rating IS NOT NULL GROUP BY p.rating")
    List<Object[]> countRatings(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    /** Đồng bộ upvotes/downvotes từ post_reactions (đếm lại nên không lệch khi nhiều request cùng lúc). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE posts SET " +
                   "upvotes = (SELECT COUNT(*) FROM post_reactions r WHERE r.post_id = :postId AND r.reaction_type = 'like'), " +
                   "downvotes = (SELECT COUNT(*) FROM post_reactions r WHERE r.post_id = :postId AND r.reaction_type = 'dislike') " +
                   "WHERE id = :postId", nativeQuery = true)
    void syncReactionCounts(@Param("postId") UUID postId);
}
