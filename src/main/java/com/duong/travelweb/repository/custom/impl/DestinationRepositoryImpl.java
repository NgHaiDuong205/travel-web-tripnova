package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.repository.custom.DestinationRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.UUID;

public class DestinationRepositoryImpl implements DestinationRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DestinationEntity> findForAdmin(String keyword, UUID countryId, Boolean active, int page, int limit) {
        String jpql = "SELECT d FROM DestinationEntity d JOIN FETCH d.country WHERE 1=1"
                + buildCondition(keyword, countryId, active) + " ORDER BY d.name";
        TypedQuery<DestinationEntity> query = entityManager.createQuery(jpql, DestinationEntity.class);
        bindParams(query, keyword, countryId, active);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countForAdmin(String keyword, UUID countryId, Boolean active) {
        String jpql = "SELECT COUNT(d) FROM DestinationEntity d WHERE 1=1" + buildCondition(keyword, countryId, active);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, keyword, countryId, active);
        return query.getSingleResult();
    }

    @Override
    public List<DestinationEntity> findPublic(String keyword, String countryCode, String continentCode, Boolean popular,
                                              Integer page, Integer limit) {
        String jpql = "SELECT d FROM DestinationEntity d JOIN FETCH d.country c WHERE d.isActive = true"
                + buildPublicCondition(keyword, countryCode, continentCode, popular) + " ORDER BY d.name";
        TypedQuery<DestinationEntity> query = entityManager.createQuery(jpql, DestinationEntity.class);
        bindPublicParams(query, keyword, countryCode, continentCode, popular);
        if (limit != null) {
            query.setFirstResult((Math.max(page == null ? 1 : page, 1) - 1) * limit);
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public long countPublic(String keyword, String countryCode, String continentCode, Boolean popular) {
        String jpql = "SELECT COUNT(d) FROM DestinationEntity d JOIN d.country c WHERE d.isActive = true"
                + buildPublicCondition(keyword, countryCode, continentCode, popular);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindPublicParams(query, keyword, countryCode, continentCode, popular);
        return query.getSingleResult();
    }

    private String buildPublicCondition(String keyword, String countryCode, String continentCode, Boolean popular) {
        StringBuilder where = new StringBuilder();
        if (keyword != null) {
            where.append(" AND (LOWER(d.name) LIKE :keyword OR LOWER(c.name) LIKE :keyword)");
        }
        if (countryCode != null) {
            where.append(" AND LOWER(c.countryCode) = :countryCode");
        }
        if (continentCode != null) {
            where.append(" AND LOWER(c.continent.code) = :continentCode");
        }
        if (popular != null) {
            where.append(" AND d.isPopular = :popular");
        }
        return where.toString();
    }

    private void bindPublicParams(TypedQuery<?> query, String keyword, String countryCode, String continentCode, Boolean popular) {
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (countryCode != null) {
            query.setParameter("countryCode", countryCode.toLowerCase());
        }
        if (continentCode != null) {
            query.setParameter("continentCode", continentCode.toLowerCase());
        }
        if (popular != null) {
            query.setParameter("popular", popular);
        }
    }

    private String buildCondition(String keyword, UUID countryId, Boolean active) {
        StringBuilder where = new StringBuilder();
        if (keyword != null) {
            where.append(" AND LOWER(d.name) LIKE :keyword");
        }
        if (countryId != null) {
            where.append(" AND d.country.id = :countryId");
        }
        if (active != null) {
            where.append(" AND d.isActive = :active");
        }
        return where.toString();
    }

    private void bindParams(TypedQuery<?> query, String keyword, UUID countryId, Boolean active) {
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (countryId != null) {
            query.setParameter("countryId", countryId);
        }
        if (active != null) {
            query.setParameter("active", active);
        }
    }
}
