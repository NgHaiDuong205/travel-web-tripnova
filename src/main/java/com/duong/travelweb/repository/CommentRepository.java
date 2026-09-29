package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CommentEntity;
import com.duong.travelweb.repository.custom.CommentRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID>, CommentRepositoryCustom {

    @Query("SELECT c FROM CommentEntity c JOIN FETCH c.user LEFT JOIN FETCH c.parentComment " +
           "WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<CommentEntity> findByPostId(@Param("postId") UUID postId);

    @Query("SELECT c FROM CommentEntity c JOIN FETCH c.user JOIN FETCH c.post WHERE c.id = :id")
    Optional<CommentEntity> findWithUserAndPost(@Param("id") UUID id);

    /** [postId, số bình luận đang hiển thị] */
    @Query("SELECT c.post.id, COUNT(c) FROM CommentEntity c WHERE c.post.id IN :postIds AND c.status = 'active' " +
           "GROUP BY c.post.id")
    List<Object[]> countActiveByPostIds(@Param("postIds") Collection<UUID> postIds);
}
