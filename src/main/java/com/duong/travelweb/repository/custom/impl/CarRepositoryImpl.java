package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.CarSearchBuilder;
import com.duong.travelweb.model.entity.CarEntity;
import com.duong.travelweb.repository.custom.CarRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public class CarRepositoryImpl implements CarRepositoryCustom {
    /** Trạng thái booking đang chiếm xe (pending thì chỉ khi còn trong hạn giữ chỗ). */
    static final String BLOCKING = "(b.status IN ('confirmed', 'checked_in') OR (b.status = 'pending' AND b.createdAt >= :holdCutoff))";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CarEntity> findCars(CarSearchBuilder criteria, LocalDateTime holdCutoff, int page, int limit) {
        String jpql = "SELECT c FROM CarEntity c LEFT JOIN FETCH c.destination d LEFT JOIN FETCH d.country WHERE 1 = 1"
                + buildCondition(criteria) + " ORDER BY " + orderBy(criteria.getSort());
        TypedQuery<CarEntity> query = entityManager.createQuery(jpql, CarEntity.class);
        bindParams(query, criteria, holdCutoff);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countCars(CarSearchBuilder criteria, LocalDateTime holdCutoff) {
        String jpql = "SELECT COUNT(c) FROM CarEntity c LEFT JOIN c.destination d WHERE 1 = 1" + buildCondition(criteria);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindParams(query, criteria, holdCutoff);
        return query.getSingleResult();
    }

    private String buildCondition(CarSearchBuilder criteria) {
        StringBuilder where = new StringBuilder();
        if (criteria.getActive() != null) {
            where.append(" AND c.isActive = :active");
        }
        if (criteria.getDestinationId() != null) {
            where.append(" AND d.id = :destinationId");
        }
        if (criteria.getKeyword() != null) {
            where.append(" AND (LOWER(c.name) LIKE :keyword OR LOWER(c.brand) LIKE :keyword OR LOWER(c.model) LIKE :keyword"
                    + " OR LOWER(c.pickupLocation) LIKE :keyword OR LOWER(d.name) LIKE :keyword)");
        }
        if (criteria.getCarType() != null) {
            where.append(" AND c.carType = :carType");
        }
        if (criteria.getBrands() != null && !criteria.getBrands().isEmpty()) {
            where.append(" AND LOWER(c.brand) IN :brands");
        }
        if (criteria.getMinSeats() != null) {
            where.append(" AND c.seats >= :minSeats");
        }
        if (criteria.getTransmission() != null) {
            where.append(" AND LOWER(c.transmission) = :transmission");
        }
        if (criteria.getFuelType() != null) {
            where.append(" AND LOWER(c.fuelType) = :fuelType");
        }
        if (criteria.getWithDriver() != null) {
            where.append(" AND c.withDriver = :withDriver");
        }
        if (criteria.getPriceMin() != null) {
            where.append(" AND c.pricePerDay >= :priceMin");
        }
        if (criteria.getPriceMax() != null) {
            where.append(" AND c.pricePerDay <= :priceMax");
        }
        if (criteria.getAvailableFrom() != null && criteria.getAvailableTo() != null) {
            where.append(" AND NOT EXISTS (SELECT b FROM CarBookingEntity b WHERE b.car = c"
                    + " AND b.pickupDate < :availableTo AND b.returnDate > :availableFrom AND " + BLOCKING + ")");
        }
        return where.toString();
    }

    /** Thứ tự cố định theo whitelist, không ghép chuỗi từ request. */
    private String orderBy(String sort) {
        if (sort == null) {
            return "c.createdAt DESC";
        }
        return switch (sort) {
            case "price_asc" -> "c.pricePerDay ASC, c.name ASC";
            case "price_desc" -> "c.pricePerDay DESC, c.name ASC";
            case "seats_desc" -> "c.seats DESC, c.pricePerDay ASC";
            case "name" -> "c.name ASC";
            default -> "c.createdAt DESC";
        };
    }

    private void bindParams(TypedQuery<?> query, CarSearchBuilder criteria, LocalDateTime holdCutoff) {
        if (criteria.getActive() != null) {
            query.setParameter("active", criteria.getActive());
        }
        if (criteria.getDestinationId() != null) {
            query.setParameter("destinationId", criteria.getDestinationId());
        }
        if (criteria.getKeyword() != null) {
            query.setParameter("keyword", "%" + criteria.getKeyword().toLowerCase() + "%");
        }
        if (criteria.getCarType() != null) {
            query.setParameter("carType", criteria.getCarType());
        }
        if (criteria.getBrands() != null && !criteria.getBrands().isEmpty()) {
            query.setParameter("brands", criteria.getBrands().stream().map(String::toLowerCase).toList());
        }
        if (criteria.getMinSeats() != null) {
            query.setParameter("minSeats", criteria.getMinSeats().shortValue());
        }
        if (criteria.getTransmission() != null) {
            query.setParameter("transmission", criteria.getTransmission().toLowerCase());
        }
        if (criteria.getFuelType() != null) {
            query.setParameter("fuelType", criteria.getFuelType().toLowerCase());
        }
        if (criteria.getWithDriver() != null) {
            query.setParameter("withDriver", criteria.getWithDriver());
        }
        if (criteria.getPriceMin() != null) {
            query.setParameter("priceMin", criteria.getPriceMin());
        }
        if (criteria.getPriceMax() != null) {
            query.setParameter("priceMax", criteria.getPriceMax());
        }
        if (criteria.getAvailableFrom() != null && criteria.getAvailableTo() != null) {
            query.setParameter("availableFrom", criteria.getAvailableFrom());
            query.setParameter("availableTo", criteria.getAvailableTo());
            query.setParameter("holdCutoff", holdCutoff);
        }
    }
}
