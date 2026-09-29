package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.model.entity.InvoiceEntity;
import com.duong.travelweb.repository.custom.InvoiceRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;

public class InvoiceRepositoryImpl implements InvoiceRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<InvoiceEntity> findForAdmin(String keyword, LocalDate from, LocalDate to, int page, int limit) {
        String jpql = "SELECT i FROM InvoiceEntity i JOIN FETCH i.order o JOIN FETCH i.user u LEFT JOIN FETCH i.payment WHERE 1 = 1"
                + buildCondition(keyword, from, to) + " ORDER BY i.issuedAt DESC";
        TypedQuery<InvoiceEntity> query = entityManager.createQuery(jpql, InvoiceEntity.class);
        bindParams(query, keyword, from, to);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countForAdmin(String keyword, LocalDate from, LocalDate to) {
        String jpql = "SELECT COUNT(i) FROM InvoiceEntity i JOIN i.order o JOIN i.user u WHERE 1 = 1"
                + buildCondition(keyword, from, to);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, keyword, from, to);
        return query.getSingleResult();
    }

    private String buildCondition(String keyword, LocalDate from, LocalDate to) {
        StringBuilder where = new StringBuilder();
        if (keyword != null) {
            where.append(" AND (LOWER(i.invoiceNumber) LIKE :keyword OR LOWER(o.orderCode) LIKE :keyword" +
                    " OR LOWER(u.email) LIKE :keyword OR LOWER(u.fullName) LIKE :keyword)");
        }
        if (from != null) {
            where.append(" AND i.issuedAt >= :from");
        }
        if (to != null) {
            where.append(" AND i.issuedAt < :to");
        }
        return where.toString();
    }

    private void bindParams(TypedQuery<?> query, String keyword, LocalDate from, LocalDate to) {
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (from != null) {
            query.setParameter("from", from.atStartOfDay());
        }
        if (to != null) {
            query.setParameter("to", to.plusDays(1).atStartOfDay());
        }
    }
}
