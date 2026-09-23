package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RoomTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, UUID> {

    @Query("SELECT rt FROM RoomTypeEntity rt JOIN FETCH rt.hotel WHERE rt.id = :roomTypeId AND rt.hotel.id = :hotelId")
    Optional<RoomTypeEntity> findByIdAndHotelId(@Param("roomTypeId") UUID roomTypeId, @Param("hotelId") UUID hotelId);
}
