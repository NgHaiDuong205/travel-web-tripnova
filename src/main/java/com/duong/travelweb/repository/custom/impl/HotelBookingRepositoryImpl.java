package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.model.entity.HotelBookingEntity;
import com.duong.travelweb.repository.custom.HotelBookingRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class HotelBookingRepositoryImpl implements HotelBookingRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<HotelBookingEntity> findByUser(UUID userId, String statusGroup, int page, int limit) {
        String jpql = "SELECT b FROM HotelBookingEntity b " +
                "JOIN FETCH b.order JOIN FETCH b.hotel JOIN FETCH b.roomType LEFT JOIN FETCH b.room " +
                "WHERE b.user.id = :userId" + buildStatusCondition(statusGroup) +
                " ORDER BY b.createdAt DESC";
        TypedQuery<HotelBookingEntity> query = entityManager.createQuery(jpql, HotelBookingEntity.class);
        bindParams(query, userId, statusGroup);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countByUser(UUID userId, String statusGroup) {
        String jpql = "SELECT COUNT(b) FROM HotelBookingEntity b WHERE b.user.id = :userId" + buildStatusCondition(statusGroup);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, userId, statusGroup);
        return query.getSingleResult();
    }

    /** Điều kiện cố định theo nhóm — không nối chuỗi input của người dùng. */
    private String buildStatusCondition(String statusGroup) {
        if (statusGroup == null) {
            return "";
        }
        return switch (statusGroup) {
            case "upcoming" -> " AND b.status IN ('pending', 'confirmed', 'checked_in') AND b.checkOutDate >= :today";
            case "completed" -> " AND (b.status IN ('checked_out', 'completed') OR (b.status = 'confirmed' AND b.checkOutDate < :today))";
            case "cancelled" -> " AND b.status IN ('cancelled', 'refunded', 'no_show')";
            case "pending" -> " AND b.status = 'pending'";
            default -> "";
        };
    }

    private void bindParams(TypedQuery<?> query, UUID userId, String statusGroup) {
        query.setParameter("userId", userId);
        if ("upcoming".equals(statusGroup) || "completed".equals(statusGroup)) {
            query.setParameter("today", LocalDate.now());
        }
    }
}
