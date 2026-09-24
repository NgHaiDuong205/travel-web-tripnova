package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.repository.custom.HotelRepositoryCustom;
import com.duong.travelweb.model.entity.HotelEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HotelRepositoryImpl implements HotelRepositoryCustom {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<HotelEntity> findHotel(HotelSearchBuilder hotelSearchBuilder) {
        HotelSearchSql searchSql = buildSearchSql(hotelSearchBuilder, "SELECT DISTINCT h.* FROM hotels h ");

        Query query = entityManager.createNativeQuery(searchSql.sql.toString(), HotelEntity.class);
        bindParameters(query, searchSql.params);

        Integer page = hotelSearchBuilder.getPage();
        Integer limit = hotelSearchBuilder.getLimit();
        if (page != null || limit != null) {
            if (page == null || page < 1) page = 1;
            if (limit == null || limit < 1) limit = DEFAULT_LIMIT;
            if (limit > MAX_LIMIT) limit = MAX_LIMIT;
            int offset = (page - 1) * limit;
            query.setFirstResult(offset);
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }

    @Override
    public long countHotel(HotelSearchBuilder hotelSearchBuilder) {
        HotelSearchSql searchSql = buildSearchSql(hotelSearchBuilder, "SELECT COUNT(DISTINCT h.id) FROM hotels h ");

        Query query = entityManager.createNativeQuery(searchSql.sql.toString());
        bindParameters(query, searchSql.params);

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<HotelEntity> findForAdmin(String keyword, UUID destinationId, Boolean active, int page, int limit) {
        String jpql = "SELECT h FROM HotelEntity h LEFT JOIN FETCH h.destination d LEFT JOIN FETCH d.country WHERE 1=1"
                + buildAdminCondition(keyword, destinationId, active) + " ORDER BY h.createdAt DESC, h.name";
        TypedQuery<HotelEntity> query = entityManager.createQuery(jpql, HotelEntity.class);
        bindAdminParams(query, keyword, destinationId, active);
        query.setFirstResult((Math.max(page, 1) - 1) * limit);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countForAdmin(String keyword, UUID destinationId, Boolean active) {
        String jpql = "SELECT COUNT(h) FROM HotelEntity h WHERE 1=1" + buildAdminCondition(keyword, destinationId, active);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindAdminParams(query, keyword, destinationId, active);
        return query.getSingleResult();
    }

    private String buildAdminCondition(String keyword, UUID destinationId, Boolean active) {
        StringBuilder where = new StringBuilder();
        if (keyword != null) {
            where.append(" AND (LOWER(h.name) LIKE :keyword OR LOWER(h.address) LIKE :keyword)");
        }
        if (destinationId != null) {
            where.append(" AND h.destination.id = :destinationId");
        }
        if (active != null) {
            where.append(" AND h.isActive = :active");
        }
        return where.toString();
    }

    private void bindAdminParams(TypedQuery<?> query, String keyword, UUID destinationId, Boolean active) {
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (destinationId != null) {
            query.setParameter("destinationId", destinationId);
        }
        if (active != null) {
            query.setParameter("active", active);
        }
    }

    private void bindParameters(Query query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private HotelSearchSql buildSearchSql(HotelSearchBuilder hotelSearchBuilder, String selectClause) {
        StringBuilder sql = new StringBuilder(selectClause);
        sql.append("LEFT JOIN destinations d ON h.destination_id = d.id ");
        sql.append("LEFT JOIN countries c ON d.country_id = c.id ");
        sql.append("WHERE h.is_active = true ");

        Map<String, Object> params = new LinkedHashMap<>();

        if (hotelSearchBuilder.getName() != null && !hotelSearchBuilder.getName().trim().isEmpty()) {
            sql.append("AND h.name ILIKE :name ");
            params.put("name", "%" + hotelSearchBuilder.getName().trim() + "%");
        }
        if (hotelSearchBuilder.getStarRating() != null) {
            sql.append("AND h.star_rating = :starRating ");
            params.put("starRating", hotelSearchBuilder.getStarRating());
        }
        if (hotelSearchBuilder.getMinStarRating() != null) {
            sql.append("AND h.star_rating >= :minStarRating ");
            params.put("minStarRating", hotelSearchBuilder.getMinStarRating());
        }
        if (hotelSearchBuilder.getMaxStarRating() != null) {
            sql.append("AND h.star_rating <= :maxStarRating ");
            params.put("maxStarRating", hotelSearchBuilder.getMaxStarRating());
        }
        if (hotelSearchBuilder.getTotalRooms() != null) {
            sql.append("AND h.total_rooms >= :totalRooms ");
            params.put("totalRooms", hotelSearchBuilder.getTotalRooms());
        }
        if (hotelSearchBuilder.getDestinationName() != null && !hotelSearchBuilder.getDestinationName().trim().isEmpty()) {
            sql.append("AND d.name ILIKE :destinationName ");
            params.put("destinationName", "%" + hotelSearchBuilder.getDestinationName().trim() + "%");
        }
        if (hotelSearchBuilder.getCountryName() != null && !hotelSearchBuilder.getCountryName().trim().isEmpty()) {
            sql.append("AND c.name ILIKE :countryName ");
            params.put("countryName", "%" + hotelSearchBuilder.getCountryName().trim() + "%");
        }
        if (hotelSearchBuilder.getDestinationId() != null) {
            sql.append("AND h.destination_id = :destinationId ");
            params.put("destinationId", hotelSearchBuilder.getDestinationId());
        }
        if (hotelSearchBuilder.getManagedById() != null) {
            sql.append("AND h.managed_by = :managedById ");
            params.put("managedById", hotelSearchBuilder.getManagedById());
        }
        if (hotelSearchBuilder.getId() != null) {
            sql.append("AND h.id = :id ");
            params.put("id", hotelSearchBuilder.getId());
        }

        // Amenities dynamic filter using subquery
        boolean hasAmenities = hotelSearchBuilder.getAmenities() != null && !hotelSearchBuilder.getAmenities().isEmpty();
        if (hasAmenities) {
            boolean isUuid = false;
            List<UUID> amenityIds = new ArrayList<>();
            List<String> amenityNames = new ArrayList<>();

            for (String item : hotelSearchBuilder.getAmenities()) {
                if (item == null || item.trim().isEmpty()) continue;
                try {
                    amenityIds.add(UUID.fromString(item.trim()));
                    isUuid = true;
                } catch (IllegalArgumentException e) {
                    amenityNames.add(item.trim().toLowerCase());
                }
            }

            if (!amenityIds.isEmpty() || !amenityNames.isEmpty()) {
                if (isUuid) {
                    sql.append("AND (SELECT COUNT(distinct ha.amenity_id) FROM hotel_amenities ha ")
                       .append("WHERE ha.hotel_id = h.id AND ha.amenity_id IN (:amenityIds)) = :amenityCount ");
                    params.put("amenityIds", amenityIds);
                    params.put("amenityCount", amenityIds.size());
                } else {
                    sql.append("AND (SELECT COUNT(distinct ha.amenity_id) FROM hotel_amenities ha ")
                       .append("JOIN amenities a ON ha.amenity_id = a.id ")
                       .append("WHERE ha.hotel_id = h.id AND LOWER(a.name) IN (:amenityNames)) = :amenityCount ");
                    params.put("amenityNames", amenityNames);
                    params.put("amenityCount", amenityNames.size());
                }
            }
        }

        return new HotelSearchSql(sql, params);
    }

    private static class HotelSearchSql {
        private final StringBuilder sql;
        private final Map<String, Object> params;

        private HotelSearchSql(StringBuilder sql, Map<String, Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
