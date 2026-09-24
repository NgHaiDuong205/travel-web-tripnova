package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.model.entity.LandmarkEntity;
import com.duong.travelweb.repository.custom.LandmarkRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.UUID;

public class LandmarkRepositoryImpl implements LandmarkRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<LandmarkEntity> findForAdmin(String keyword, UUID destinationId, String category, Boolean active, int page, int limit) {
        String jpql = "SELECT l FROM LandmarkEntity l JOIN FETCH l.destination WHERE 1=1"
                + buildCondition(keyword, destinationId, category, active) + " ORDER BY l.name";
        TypedQuery<LandmarkEntity> query = entityManager.createQuery(jpql, LandmarkEntity.class);
        bindParams(query, keyword, destinationId, category, active);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countForAdmin(String keyword, UUID destinationId, String category, Boolean active) {
        String jpql = "SELECT COUNT(l) FROM LandmarkEntity l WHERE 1=1" + buildCondition(keyword, destinationId, category, active);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, keyword, destinationId, category, active);
        return query.getSingleResult();
    }

    private String buildCondition(String keyword, UUID destinationId, String category, Boolean active) {
        StringBuilder where = new StringBuilder();
        if (keyword != null) {
            where.append(" AND LOWER(l.name) LIKE :keyword");
        }
        if (destinationId != null) {
            where.append(" AND l.destination.id = :destinationId");
        }
        if (category != null) {
            // category là Postgres ENUM → so sánh dạng chuỗi
            where.append(" AND CAST(l.category AS string) = :category");
        }
        if (active != null) {
            where.append(" AND l.isActive = :active");
        }
        return where.toString();
    }

    private void bindParams(TypedQuery<?> query, String keyword, UUID destinationId, String category, Boolean active) {
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (destinationId != null) {
            query.setParameter("destinationId", destinationId);
        }
        if (category != null) {
            query.setParameter("category", category);
        }
        if (active != null) {
            query.setParameter("active", active);
        }
    }
}
