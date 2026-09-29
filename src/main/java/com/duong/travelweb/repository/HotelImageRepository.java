package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.HotelImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HotelImageRepository extends JpaRepository<HotelImageEntity, UUID> {

    @Query("SELECT i FROM HotelImageEntity i WHERE i.hotelId = :hotelId ORDER BY i.sortOrder, i.createdAt, i.id")
    List<HotelImageEntity> findByHotelId(@Param("hotelId") UUID hotelId);

    @Query("SELECT COUNT(i) FROM HotelImageEntity i WHERE i.hotelId = :hotelId")
    long countByHotelId(@Param("hotelId") UUID hotelId);

    @Query("SELECT COALESCE(MAX(i.sortOrder), -1) FROM HotelImageEntity i WHERE i.hotelId = :hotelId")
    int maxSortOrder(@Param("hotelId") UUID hotelId);
}
