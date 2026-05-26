package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.repository.custom.HotelRepositoryCustom;
import com.duong.travelweb.model.entity.HotelEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HotelRepositoryImpl implements HotelRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    @SuppressWarnings("unchecked")
    public List<HotelEntity> findHotel(HotelSearchBuilder hotelSearchBuilder) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT h.* FROM hotels h ");
        sql.append("LEFT JOIN destinations d ON h.destination_id = d.id ");
        sql.append("LEFT JOIN countries c ON d.country_id = c.id ");
        sql.append("WHERE 1=1 ");

        if (hotelSearchBuilder.getName() != null && !hotelSearchBuilder.getName().trim().isEmpty()) {
            sql.append("AND LOWER(h.name) LIKE :name ");
        }
        if (hotelSearchBuilder.getStarRating() != null) {
            sql.append("AND h.star_rating = :starRating ");
        }
        if (hotelSearchBuilder.getMinStarRating() != null) {
            sql.append("AND h.star_rating >= :minStarRating ");
        }
        if (hotelSearchBuilder.getMaxStarRating() != null) {
            sql.append("AND h.star_rating <= :maxStarRating ");
        }
        if (hotelSearchBuilder.getTotalRooms() != null) {
            sql.append("AND h.total_rooms >= :totalRooms ");
        }
        if (hotelSearchBuilder.getDestinationName() != null && !hotelSearchBuilder.getDestinationName().trim().isEmpty()) {
            sql.append("AND LOWER(d.name) LIKE :destinationName ");
        }
        if (hotelSearchBuilder.getCountryName() != null && !hotelSearchBuilder.getCountryName().trim().isEmpty()) {
            sql.append("AND LOWER(c.name) LIKE :countryName ");
        }
        if (hotelSearchBuilder.getDestinationId() != null) {
            sql.append("AND h.destination_id = :destinationId ");
        }
        if (hotelSearchBuilder.getManagedById() != null) {
            sql.append("AND h.managed_by = :managedById ");
        }
        if (hotelSearchBuilder.getId() != null) {
            sql.append("AND h.id = :id ");
        }

        // Amenities dynamic filter using subquery
        boolean hasAmenities = hotelSearchBuilder.getAmenities() != null && !hotelSearchBuilder.getAmenities().isEmpty();
        boolean isUuid = false;
        List<UUID> amenityIds = new ArrayList<>();
        List<String> amenityNames = new ArrayList<>();

        if (hasAmenities) {
            for (String item : hotelSearchBuilder.getAmenities()) {
                if (item == null || item.trim().isEmpty()) continue;
                try {
                    amenityIds.add(UUID.fromString(item.trim()));
                    isUuid = true;
                } catch (IllegalArgumentException e) {
                    amenityNames.add(item.trim().toLowerCase());
                }
            }
            
            // Check if we actually have valid items to filter
            if (!amenityIds.isEmpty() || !amenityNames.isEmpty()) {
                if (isUuid) {
                    sql.append("AND (SELECT COUNT(distinct ha.amenity_id) FROM hotel_amenities ha ")
                       .append("WHERE ha.hotel_id = h.id AND ha.amenity_id IN (:amenityIds)) = :amenityCount ");
                } else {
                    sql.append("AND (SELECT COUNT(distinct ha.amenity_id) FROM hotel_amenities ha ")
                       .append("JOIN amenities a ON ha.amenity_id = a.id ")
                       .append("WHERE ha.hotel_id = h.id AND LOWER(a.name) IN (:amenityNames)) = :amenityCount ");
                }
            } else {
                hasAmenities = false;
            }
        }

        Query query = entityManager.createNativeQuery(sql.toString(), HotelEntity.class);

        // Apply Pagination
        Integer page = hotelSearchBuilder.getPage();
        Integer limit = hotelSearchBuilder.getLimit();
        System.out.println("--- HotelRepositoryImpl.findHotel ---");
        System.out.println("Builder page: " + page + ", limit: " + limit);
        if (page != null || limit != null) {
            if (page == null || page < 1) page = 1;
            if (limit == null || limit < 1) limit = 10;
            int offset = (page - 1) * limit;
            System.out.println("Applying setFirstResult(" + offset + ") and setMaxResults(" + limit + ")");
            query.setFirstResult(offset);
            query.setMaxResults(limit);
        }

        // Bind parameters safely to prevent SQL Injection
        if (hotelSearchBuilder.getName() != null && !hotelSearchBuilder.getName().trim().isEmpty()) {
            query.setParameter("name", "%" + hotelSearchBuilder.getName().trim().toLowerCase() + "%");
        }
        if (hotelSearchBuilder.getStarRating() != null) {
            query.setParameter("starRating", hotelSearchBuilder.getStarRating());
        }
        if (hotelSearchBuilder.getMinStarRating() != null) {
            query.setParameter("minStarRating", hotelSearchBuilder.getMinStarRating());
        }
        if (hotelSearchBuilder.getMaxStarRating() != null) {
            query.setParameter("maxStarRating", hotelSearchBuilder.getMaxStarRating());
        }
        if (hotelSearchBuilder.getTotalRooms() != null) {
            query.setParameter("totalRooms", hotelSearchBuilder.getTotalRooms());
        }
        if (hotelSearchBuilder.getDestinationName() != null && !hotelSearchBuilder.getDestinationName().trim().isEmpty()) {
            query.setParameter("destinationName", "%" + hotelSearchBuilder.getDestinationName().trim().toLowerCase() + "%");
        }
        if (hotelSearchBuilder.getCountryName() != null && !hotelSearchBuilder.getCountryName().trim().isEmpty()) {
            query.setParameter("countryName", "%" + hotelSearchBuilder.getCountryName().trim().toLowerCase() + "%");
        }
        if (hotelSearchBuilder.getDestinationId() != null) {
            query.setParameter("destinationId", hotelSearchBuilder.getDestinationId());
        }
        if (hotelSearchBuilder.getManagedById() != null) {
            query.setParameter("managedById", hotelSearchBuilder.getManagedById());
        }
        if (hotelSearchBuilder.getId() != null) {
            query.setParameter("id", hotelSearchBuilder.getId());
        }
        if (hasAmenities) {
            if (isUuid) {
                query.setParameter("amenityIds", amenityIds);
                query.setParameter("amenityCount", amenityIds.size());
            } else {
                query.setParameter("amenityNames", amenityNames);
                query.setParameter("amenityCount", amenityNames.size());
            }
        }

        return query.getResultList();
    }
}
