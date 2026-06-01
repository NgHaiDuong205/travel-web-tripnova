package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.duong.travelweb.repository.custom.RoomRepositoryCustom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, UUID>, RoomRepositoryCustom {
    @Query("SELECT r FROM RoomEntity r WHERE r.roomType.hotel.id = :hotelId AND r.status = 'available'")
    List<RoomEntity> findByHotelId(@Param("hotelId") UUID hotelId);

    @Query("SELECT r FROM RoomEntity r WHERE r.id = :roomId AND r.roomType.hotel.id = :hotelId")
    Optional<RoomEntity> findByIdAndHotelId(@Param("roomId") UUID roomId, @Param("hotelId") UUID hotelId);
}
