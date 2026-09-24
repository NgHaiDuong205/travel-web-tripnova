package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RoomTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, UUID> {

    @Query("SELECT rt FROM RoomTypeEntity rt JOIN FETCH rt.hotel WHERE rt.id = :roomTypeId AND rt.hotel.id = :hotelId")
    Optional<RoomTypeEntity> findByIdAndHotelId(@Param("roomTypeId") UUID roomTypeId, @Param("hotelId") UUID hotelId);

    @Query("SELECT rt FROM RoomTypeEntity rt WHERE rt.hotel.id = :hotelId AND rt.isActive = true ORDER BY rt.pricePerNight")
    List<RoomTypeEntity> findActiveByHotelId(@Param("hotelId") UUID hotelId);

    /** [roomTypeId, số phòng] */
    @Query("SELECT r.roomType.id, COUNT(r) FROM RoomEntity r WHERE r.roomType.id IN :roomTypeIds GROUP BY r.roomType.id")
    List<Object[]> countRoomsByRoomTypeIds(@Param("roomTypeIds") List<UUID> roomTypeIds);

    /** Tất cả hạng phòng của khách sạn (kể cả đã ẩn) — cho admin. */
    @Query("SELECT rt FROM RoomTypeEntity rt WHERE rt.hotel.id = :hotelId ORDER BY rt.isActive DESC, rt.pricePerNight")
    List<RoomTypeEntity> findAllByHotelId(@Param("hotelId") UUID hotelId);

    /** [hotelId, số hạng phòng] */
    @Query("SELECT rt.hotel.id, COUNT(rt) FROM RoomTypeEntity rt WHERE rt.hotel.id IN :hotelIds GROUP BY rt.hotel.id")
    List<Object[]> countByHotelIds(@Param("hotelIds") List<UUID> hotelIds);
}
