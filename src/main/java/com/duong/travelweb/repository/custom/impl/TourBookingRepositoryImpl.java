package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.model.entity.TourBookingEntity;
import com.duong.travelweb.repository.custom.TourBookingRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class TourBookingRepositoryImpl implements TourBookingRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<TourBookingEntity> findForAdmin(String status, String keyword, int page, int limit) {
        String jpql = "SELECT b FROM TourBookingEntity b JOIN FETCH b.tour t LEFT JOIN FETCH t.destination JOIN FETCH b.order o "
                + "JOIN FETCH b.user u WHERE 1 = 1" + buildCondition(status, keyword) + " ORDER BY b.createdAt DESC";
        TypedQuery<TourBookingEntity> query = entityManager.createQuery(jpql, TourBookingEntity.class);
        bindParams(query, status, keyword);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countForAdmin(String status, String keyword) {
        String jpql = "SELECT COUNT(b) FROM TourBookingEntity b JOIN b.tour t JOIN b.order o JOIN b.user u WHERE 1 = 1"
                + buildCondition(status, keyword);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, status, keyword);
        return query.getSingleResult();
    }

    private String buildCondition(String status, String keyword) {
        StringBuilder where = new StringBuilder();
        if (status != null) {
            where.append(" AND b.status = :status");
        }
        if (keyword != null) {
            where.append(" AND (LOWER(o.orderCode) LIKE :keyword OR LOWER(u.email) LIKE :keyword OR LOWER(u.fullName) LIKE :keyword"
                    + " OR LOWER(b.contactName) LIKE :keyword OR LOWER(t.name) LIKE :keyword)");
        }
        return where.toString();
    }

    private void bindParams(TypedQuery<?> query, String status, String keyword) {
        if (status != null) {
            query.setParameter("status", status);
        }
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
    }
}
