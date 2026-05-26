package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.RoomSearchBuilder;
import com.duong.travelweb.model.entity.RoomEntity;
import com.duong.travelweb.repository.custom.RoomRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;
import java.util.UUID;

public class RoomRepositoryImpl implements RoomRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<RoomEntity> searchRooms(UUID hotelId, RoomSearchBuilder searchBuilder) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT r.* FROM rooms r ");
        sql.append("JOIN room_types rt ON r.room_type_id = rt.id ");
        sql.append("WHERE rt.hotel_id = :hotelId ");

        if (searchBuilder.getRoomTypeName() != null && !searchBuilder.getRoomTypeName().trim().isEmpty()) {
            sql.append("AND LOWER(rt.name) LIKE :roomTypeName ");
        }

        if (searchBuilder.getMaxOccupancy() != null) {
            sql.append("AND rt.max_occupancy >= :maxOccupancy ");
        }

        if (searchBuilder.getMinPrice() != null) {
            sql.append("AND rt.price_per_night >= :minPrice ");
        }

        if (searchBuilder.getMaxPrice() != null) {
            sql.append("AND rt.price_per_night <= :maxPrice ");
        }

        // Date availability filter
        if (searchBuilder.getCheckIn() != null && searchBuilder.getCheckOut() != null) {
            sql.append("AND NOT EXISTS ( ");
            sql.append("  SELECT 1 FROM room_availability ra ");
            sql.append("  WHERE ra.room_id = r.id ");
            sql.append("    AND ra.date >= :checkIn ");
            sql.append("    AND ra.date < :checkOut ");
            sql.append("    AND ra.status IN ('booked'::availability_status, 'blocked'::availability_status) ");
            sql.append(") ");
        }

        // Amenities filter using join and count
        boolean hasAmenities = searchBuilder.getAmenities() != null && !searchBuilder.getAmenities().isEmpty();
        if (hasAmenities) {
            sql.append("AND ( ");
            sql.append("  SELECT COUNT(DISTINCT rta.amenity_id) ");
            sql.append("  FROM room_type_amenities rta ");
            sql.append("  JOIN amenities a ON rta.amenity_id = a.id ");
            sql.append("  WHERE rta.room_type_id = rt.id ");
            sql.append("    AND LOWER(a.name) IN (:amenities) ");
            sql.append(") = :amenityCount ");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), RoomEntity.class);
        query.setParameter("hotelId", hotelId);

        if (searchBuilder.getRoomTypeName() != null && !searchBuilder.getRoomTypeName().trim().isEmpty()) {
            query.setParameter("roomTypeName", "%" + searchBuilder.getRoomTypeName().trim().toLowerCase() + "%");
        }

        if (searchBuilder.getMaxOccupancy() != null) {
            query.setParameter("maxOccupancy", searchBuilder.getMaxOccupancy());
        }

        if (searchBuilder.getMinPrice() != null) {
            query.setParameter("minPrice", searchBuilder.getMinPrice());
        }

        if (searchBuilder.getMaxPrice() != null) {
            query.setParameter("maxPrice", searchBuilder.getMaxPrice());
        }

        if (searchBuilder.getCheckIn() != null && searchBuilder.getCheckOut() != null) {
            query.setParameter("checkIn", searchBuilder.getCheckIn());
            query.setParameter("checkOut", searchBuilder.getCheckOut());
        }

        if (hasAmenities) {
            java.util.List<String> lowercaseAmenities = new java.util.ArrayList<>();
            for (String a : searchBuilder.getAmenities()) {
                if (a != null && !a.trim().isEmpty()) {
                    lowercaseAmenities.add(a.trim().toLowerCase());
                }
            }
            query.setParameter("amenities", lowercaseAmenities);
            query.setParameter("amenityCount", lowercaseAmenities.size());
        }

        return query.getResultList();
    }
}
