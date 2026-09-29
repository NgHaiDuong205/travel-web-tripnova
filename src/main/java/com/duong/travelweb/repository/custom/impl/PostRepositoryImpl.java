package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.PostSearchBuilder;
import com.duong.travelweb.model.entity.PostEntity;
import com.duong.travelweb.repository.custom.PostRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class PostRepositoryImpl implements PostRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PostEntity> findPosts(PostSearchBuilder criteria, int page, int limit) {
        String jpql = "SELECT p FROM PostEntity p JOIN FETCH p.user u WHERE 1 = 1" + buildCondition(criteria)
                + " ORDER BY " + orderBy(criteria.getSort());
        TypedQuery<PostEntity> query = entityManager.createQuery(jpql, PostEntity.class);
        bindParams(query, criteria);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countPosts(PostSearchBuilder criteria) {
        String jpql = "SELECT COUNT(p) FROM PostEntity p JOIN p.user u WHERE 1 = 1" + buildCondition(criteria);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, criteria);
        return query.getSingleResult();
    }

    private String buildCondition(PostSearchBuilder criteria) {
        StringBuilder where = new StringBuilder();
        if (criteria.getEntityType() != null) {
            where.append(" AND p.entityType = :entityType");
        }
        if (criteria.getEntityId() != null) {
            where.append(" AND p.entityId = :entityId");
        }
        if (criteria.getUserId() != null) {
            where.append(" AND u.id = :userId");
        }
        if (criteria.getStatus() != null) {
            where.append(" AND p.status = :status");
        }
        if (criteria.getRating() != null) {
            where.append(" AND p.rating = :rating");
        }
        if (criteria.getKeyword() != null) {
            where.append(" AND (LOWER(p.title) LIKE :keyword OR LOWER(p.content) LIKE :keyword OR LOWER(u.fullName) LIKE :keyword)");
        }
        return where.toString();
    }

    /** Thứ tự cố định theo whitelist, không ghép chuỗi từ request. */
    private String orderBy(String sort) {
        if (sort == null) {
            return "p.createdAt DESC";
        }
        return switch (sort) {
            case "oldest" -> "p.createdAt ASC";
            case "top" -> "(p.upvotes - p.downvotes) DESC, p.createdAt DESC";
            case "rating_high" -> "p.rating DESC NULLS LAST, p.createdAt DESC";
            case "rating_low" -> "p.rating ASC NULLS LAST, p.createdAt DESC";
            default -> "p.createdAt DESC";
        };
    }

    private void bindParams(TypedQuery<?> query, PostSearchBuilder criteria) {
        if (criteria.getEntityType() != null) {
            query.setParameter("entityType", criteria.getEntityType());
        }
        if (criteria.getEntityId() != null) {
            query.setParameter("entityId", criteria.getEntityId());
        }
        if (criteria.getUserId() != null) {
            query.setParameter("userId", criteria.getUserId());
        }
        if (criteria.getStatus() != null) {
            query.setParameter("status", criteria.getStatus());
        }
        if (criteria.getRating() != null) {
            query.setParameter("rating", criteria.getRating());
        }
        if (criteria.getKeyword() != null) {
            query.setParameter("keyword", "%" + criteria.getKeyword().toLowerCase() + "%");
        }
    }
}
