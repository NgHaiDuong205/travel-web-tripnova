package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.FlightSearchBuilder;
import com.duong.travelweb.model.entity.FlightEntity;
import com.duong.travelweb.repository.custom.FlightRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public class FlightRepositoryImpl implements FlightRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<FlightEntity> findFlights(FlightSearchBuilder criteria, int page, int limit) {
        String jpql = "SELECT f FROM FlightEntity f WHERE 1 = 1" + buildCondition(criteria) + " ORDER BY " + orderBy(criteria.getSort());
        TypedQuery<FlightEntity> query = entityManager.createQuery(jpql, FlightEntity.class);
        bindParams(query, criteria);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countFlights(FlightSearchBuilder criteria) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(f) FROM FlightEntity f WHERE 1 = 1" + buildCondition(criteria), Long.class);
        bindParams(query, criteria);
        return query.getSingleResult();
    }

    /** Điều kiện ghế trống (theo hạng nếu có) dùng cho lọc số khách / giá. */
    private String seatCondition(FlightSearchBuilder criteria) {
        return "s.flight = f AND s.status = 'available'" + (criteria.getSeatClass() != null ? " AND s.seatClass = :seatClass" : "");
    }

    private String buildCondition(FlightSearchBuilder criteria) {
        StringBuilder where = new StringBuilder();
        if (Boolean.TRUE.equals(criteria.getUpcomingActiveOnly())) {
            where.append(" AND f.isActive = true AND f.departureTime > :now");
        }
        if (criteria.getFrom() != null) {
            where.append(" AND (UPPER(f.departureAirportCode) = :fromCode OR LOWER(f.departureCity) LIKE :fromLike)");
        }
        if (criteria.getTo() != null) {
            where.append(" AND (UPPER(f.arrivalAirportCode) = :toCode OR LOWER(f.arrivalCity) LIKE :toLike)");
        }
        if (criteria.getDepartDate() != null) {
            where.append(" AND f.departureTime >= :dayStart AND f.departureTime < :dayEnd");
        }
        if (criteria.getAirlines() != null && !criteria.getAirlines().isEmpty()) {
            where.append(" AND LOWER(f.airline) IN :airlines");
        }
        if (criteria.getKeyword() != null) {
            where.append(" AND (LOWER(f.flightNumber) LIKE :keyword OR LOWER(f.airline) LIKE :keyword"
                    + " OR LOWER(f.departureCity) LIKE :keyword OR LOWER(f.arrivalCity) LIKE :keyword"
                    + " OR LOWER(f.departureAirportCode) LIKE :keyword OR LOWER(f.arrivalAirportCode) LIKE :keyword)");
        }
        if (criteria.getTimeOfDay() != null) {
            where.append(switch (criteria.getTimeOfDay()) {
                case "morning" -> " AND EXTRACT(HOUR FROM f.departureTime) >= 5 AND EXTRACT(HOUR FROM f.departureTime) < 12";
                case "afternoon" -> " AND EXTRACT(HOUR FROM f.departureTime) >= 12 AND EXTRACT(HOUR FROM f.departureTime) < 17";
                case "evening" -> " AND EXTRACT(HOUR FROM f.departureTime) >= 17 AND EXTRACT(HOUR FROM f.departureTime) < 21";
                default -> " AND (EXTRACT(HOUR FROM f.departureTime) >= 21 OR EXTRACT(HOUR FROM f.departureTime) < 5)";
            });
        }
        if (criteria.getSeatClass() != null || criteria.getPassengers() != null) {
            where.append(" AND (SELECT COUNT(s) FROM FlightSeatEntity s WHERE ").append(seatCondition(criteria)).append(") >= :pax");
        }
        if (criteria.getPriceMax() != null) {
            where.append(" AND EXISTS (SELECT s FROM FlightSeatEntity s WHERE ").append(seatCondition(criteria))
                    .append(" AND s.price <= :priceMax)");
        }
        return where.toString();
    }

    /** Thứ tự cố định theo whitelist, không ghép chuỗi từ request. */
    private String orderBy(String sort) {
        if (sort == null) {
            return "f.departureTime ASC";
        }
        return switch (sort) {
            case "price_asc" -> "f.basePrice ASC, f.departureTime ASC";
            case "price_desc" -> "f.basePrice DESC, f.departureTime ASC";
            case "duration" -> "f.durationMinutes ASC NULLS LAST, f.departureTime ASC";
            case "newest" -> "f.createdAt DESC";
            default -> "f.departureTime ASC";
        };
    }

    private void bindParams(TypedQuery<?> query, FlightSearchBuilder criteria) {
        if (Boolean.TRUE.equals(criteria.getUpcomingActiveOnly())) {
            query.setParameter("now", LocalDateTime.now());
        }
        if (criteria.getFrom() != null) {
            query.setParameter("fromCode", criteria.getFrom().toUpperCase());
            query.setParameter("fromLike", "%" + criteria.getFrom().toLowerCase() + "%");
        }
        if (criteria.getTo() != null) {
            query.setParameter("toCode", criteria.getTo().toUpperCase());
            query.setParameter("toLike", "%" + criteria.getTo().toLowerCase() + "%");
        }
        if (criteria.getDepartDate() != null) {
            query.setParameter("dayStart", criteria.getDepartDate().atStartOfDay());
            query.setParameter("dayEnd", criteria.getDepartDate().plusDays(1).atStartOfDay());
        }
        if (criteria.getAirlines() != null && !criteria.getAirlines().isEmpty()) {
            query.setParameter("airlines", criteria.getAirlines().stream().map(String::toLowerCase).toList());
        }
        if (criteria.getKeyword() != null) {
            query.setParameter("keyword", "%" + criteria.getKeyword().toLowerCase() + "%");
        }
        if (criteria.getSeatClass() != null) {
            query.setParameter("seatClass", criteria.getSeatClass());
        }
        if (criteria.getSeatClass() != null || criteria.getPassengers() != null) {
            query.setParameter("pax", (long) (criteria.getPassengers() == null ? 1 : criteria.getPassengers()));
        }
        if (criteria.getPriceMax() != null) {
            query.setParameter("priceMax", criteria.getPriceMax());
        }
    }
}
