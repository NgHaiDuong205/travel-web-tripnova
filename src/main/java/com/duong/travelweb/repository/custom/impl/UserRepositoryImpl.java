package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.custom.UserRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class UserRepositoryImpl implements UserRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UserEntity> findForAdmin(String keyword, String role, Boolean active, int page, int limit) {
        String jpql = "SELECT u FROM UserEntity u WHERE u.deletedAt IS NULL" + buildCondition(keyword, role, active) +
                " ORDER BY u.createdAt DESC";
        TypedQuery<UserEntity> query = entityManager.createQuery(jpql, UserEntity.class);
        bindParams(query, keyword, role, active);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countForAdmin(String keyword, String role, Boolean active) {
        String jpql = "SELECT COUNT(u) FROM UserEntity u WHERE u.deletedAt IS NULL" + buildCondition(keyword, role, active);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, keyword, role, active);
        return query.getSingleResult();
    }

    private String buildCondition(String keyword, String role, Boolean active) {
        StringBuilder where = new StringBuilder();
        if (keyword != null) {
            where.append(" AND (LOWER(u.email) LIKE :keyword OR LOWER(u.fullName) LIKE :keyword OR u.phone LIKE :keyword)");
        }
        if (role != null) {
            where.append(" AND EXISTS (SELECT ur FROM UserRoleEntity ur WHERE ur.user = u AND ur.role.name = :role)");
        }
        if (active != null) {
            where.append(" AND u.isActive = :active");
        }
        return where.toString();
    }

    private void bindParams(TypedQuery<?> query, String keyword, String role, Boolean active) {
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (role != null) {
            query.setParameter("role", role);
        }
        if (active != null) {
            query.setParameter("active", active);
        }
    }
}
