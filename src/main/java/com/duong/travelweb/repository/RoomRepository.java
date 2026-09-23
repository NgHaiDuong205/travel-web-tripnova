package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.duong.travelweb.repository.custom.RoomRepositoryCustom;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, UUID>, RoomRepositoryCustom {
    @Query("SELECT r FROM RoomEntity r WHERE r.roomType.hotel.id = :hotelId AND r.status = 'available'")
    List<RoomEntity> findByHotelId(@Param("hotelId") UUID hotelId);

    @Query("SELECT r FROM RoomEntity r WHERE r.id = :roomId AND r.roomType.hotel.id = :hotelId")
    Optional<RoomEntity> findByIdAndHotelId(@Param("roomId") UUID roomId, @Param("hotelId") UUID hotelId);

    @Query("SELECT COUNT(r) FROM RoomEntity r WHERE r.roomType.hotel.id = :hotelId " +
           "AND r.status = 'available' " +
           "AND NOT EXISTS (" +
           "  SELECT ra FROM RoomAvailabilityEntity ra " +
           "  WHERE ra.room = r " +
           "    AND ra.date >= :checkIn " +
           "    AND ra.date < :checkOut " +
           "    AND ra.status IN ('booked', 'blocked')" +
           ")")
    Long countAvailableRoomsByHotelIdAndDates(@Param("hotelId") UUID hotelId,
                                              @Param("checkIn") LocalDate checkIn,
                                              @Param("checkOut") LocalDate checkOut);

    @Query("SELECT COUNT(r) FROM RoomEntity r WHERE r.roomType.hotel.id = :hotelId AND r.status = 'available'")
    Long countAvailableRoomsByHotelId(@Param("hotelId") UUID hotelId);

    @Query("SELECT r.roomType.hotel.id, COUNT(r) FROM RoomEntity r " +
           "WHERE r.roomType.hotel.id IN :hotelIds " +
           "AND r.status = 'available' " +
           "AND NOT EXISTS (" +
           "  SELECT ra FROM RoomAvailabilityEntity ra " +
           "  WHERE ra.room = r " +
           "    AND ra.date >= :checkIn " +
           "    AND ra.date < :checkOut " +
           "    AND ra.status IN ('booked', 'blocked')" +
           ") " +
           "GROUP BY r.roomType.hotel.id")
    List<Object[]> countAvailableRoomsByHotelIdsAndDates(@Param("hotelIds") List<UUID> hotelIds,
                                                         @Param("checkIn") LocalDate checkIn,
                                                         @Param("checkOut") LocalDate checkOut);

    @Query("SELECT r.roomType.hotel.id, COUNT(r) FROM RoomEntity r " +
           "WHERE r.roomType.hotel.id IN :hotelIds " +
           "AND r.status = 'available' " +
           "GROUP BY r.roomType.hotel.id")
    List<Object[]> countAvailableRoomsByHotelIds(@Param("hotelIds") List<UUID> hotelIds);

    @Query("SELECT ra.room.id, ra.status FROM RoomAvailabilityEntity ra " +
           "WHERE ra.room.roomType.hotel.id = :hotelId " +
           "AND ra.date >= :checkIn " +
           "AND ra.date < :checkOut " +
           "AND ra.status IN ('booked', 'blocked')")
    List<Object[]> findBookedOrBlockedRoomStatuses(@Param("hotelId") UUID hotelId,
                                                   @Param("checkIn") LocalDate checkIn,
                                                   @Param("checkOut") LocalDate checkOut);

    @Query("SELECT rta.roomType.id, rta.amenity.name FROM RoomTypeAmenityEntity rta WHERE rta.roomType.id IN :roomTypeIds")
    List<Object[]> findAmenityNamesByRoomTypeIds(@Param("roomTypeIds") List<UUID> roomTypeIds);
}
