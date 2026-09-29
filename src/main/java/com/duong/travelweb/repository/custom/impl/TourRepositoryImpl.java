package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.TourSearchBuilder;
import com.duong.travelweb.model.entity.TourEntity;
import com.duong.travelweb.repository.custom.TourRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class TourRepositoryImpl implements TourRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<TourEntity> findTours(TourSearchBuilder criteria, int page, int limit) {
        String jpql = "SELECT t FROM TourEntity t LEFT JOIN FETCH t.destination d LEFT JOIN FETCH d.country WHERE 1 = 1"
                + buildCondition(criteria) + " ORDER BY " + orderBy(criteria.getSort());
        TypedQuery<TourEntity> query = entityManager.createQuery(jpql, TourEntity.class);
        bindParams(query, criteria);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countTours(TourSearchBuilder criteria) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM TourEntity t LEFT JOIN t.destination d WHERE 1 = 1" + buildCondition(criteria), Long.class);
        bindParams(query, criteria);
        return query.getSingleResult();
    }

    private String buildCondition(TourSearchBuilder criteria) {
        StringBuilder where = new StringBuilder();
        if (criteria.getActive() != null) {
            where.append(" AND t.isActive = :active");
        }
        if (criteria.getDestinationId() != null) {
            where.append(" AND d.id = :destinationId");
        }
        if (criteria.getKeyword() != null) {
            where.append(" AND (LOWER(t.name) LIKE :keyword OR LOWER(t.description) LIKE :keyword"
                    + " OR LOWER(t.departureLocation) LIKE :keyword OR LOWER(d.name) LIKE :keyword)");
        }
        if (criteria.getPriceMin() != null) {
            where.append(" AND t.priceAdult >= :priceMin");
        }
        if (criteria.getPriceMax() != null) {
            where.append(" AND t.priceAdult <= :priceMax");
        }
        if (criteria.getMinDays() != null) {
            where.append(" AND t.durationDays >= :minDays");
        }
        if (criteria.getMaxDays() != null) {
            where.append(" AND t.durationDays <= :maxDays");
        }
        return where.toString();
    }

    /** Thứ tự cố định theo whitelist, không ghép chuỗi từ request. */
    private String orderBy(String sort) {
        if (sort == null) {
            return "t.createdAt DESC";
        }
        return switch (sort) {
            case "price_asc" -> "t.priceAdult ASC, t.name ASC";
            case "price_desc" -> "t.priceAdult DESC, t.name ASC";
            case "duration_asc" -> "t.durationDays ASC, t.priceAdult ASC";
            case "duration_desc" -> "t.durationDays DESC, t.priceAdult ASC";
            case "departure" -> "t.departureDate ASC NULLS LAST, t.name ASC";
            default -> "t.createdAt DESC";
        };
    }

    private void bindParams(TypedQuery<?> query, TourSearchBuilder criteria) {
        if (criteria.getActive() != null) {
            query.setParameter("active", criteria.getActive());
        }
        if (criteria.getDestinationId() != null) {
            query.setParameter("destinationId", criteria.getDestinationId());
        }
        if (criteria.getKeyword() != null) {
            query.setParameter("keyword", "%" + criteria.getKeyword().toLowerCase() + "%");
        }
        if (criteria.getPriceMin() != null) {
            query.setParameter("priceMin", criteria.getPriceMin());
        }
        if (criteria.getPriceMax() != null) {
            query.setParameter("priceMax", criteria.getPriceMax());
        }
        if (criteria.getMinDays() != null) {
            query.setParameter("minDays", criteria.getMinDays().shortValue());
        }
        if (criteria.getMaxDays() != null) {
            query.setParameter("maxDays", criteria.getMaxDays().shortValue());
        }
    }
}
