package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.model.entity.CommentEntity;
import com.duong.travelweb.repository.custom.CommentRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.UUID;

public class CommentRepositoryImpl implements CommentRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CommentEntity> findForAdmin(String status, UUID postId, String keyword, int page, int limit) {
        String jpql = "SELECT c FROM CommentEntity c JOIN FETCH c.user u JOIN FETCH c.post p WHERE 1 = 1"
                + buildCondition(status, postId, keyword) + " ORDER BY c.createdAt DESC";
        TypedQuery<CommentEntity> query = entityManager.createQuery(jpql, CommentEntity.class);
        bindParams(query, status, postId, keyword);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countForAdmin(String status, UUID postId, String keyword) {
        String jpql = "SELECT COUNT(c) FROM CommentEntity c JOIN c.user u JOIN c.post p WHERE 1 = 1"
                + buildCondition(status, postId, keyword);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, status, postId, keyword);
        return query.getSingleResult();
    }

    private String buildCondition(String status, UUID postId, String keyword) {
        StringBuilder where = new StringBuilder();
        if (status != null) {
            where.append(" AND c.status = :status");
        }
        if (postId != null) {
            where.append(" AND p.id = :postId");
        }
        if (keyword != null) {
            where.append(" AND (LOWER(c.content) LIKE :keyword OR LOWER(u.fullName) LIKE :keyword OR LOWER(u.email) LIKE :keyword)");
        }
        return where.toString();
    }

    private void bindParams(TypedQuery<?> query, String status, UUID postId, String keyword) {
        if (status != null) {
            query.setParameter("status", status);
        }
        if (postId != null) {
            query.setParameter("postId", postId);
        }
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
    }
}
