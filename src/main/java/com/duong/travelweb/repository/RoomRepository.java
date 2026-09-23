package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RoomEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.duong.travelweb.repository.custom.RoomRepositoryCustom;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /**
     * Phòng trống thực sự để đặt: không bảo trì, không có ngày booked/blocked trong [checkIn, checkOut),
     * và không bị giữ bởi booking pending khác còn trong hạn giữ chỗ (createdAt >= holdSince).
     */
    @Query("SELECT r FROM RoomEntity r WHERE r.roomType.id = :roomTypeId " +
           "AND r.status <> 'maintenance' " +
           "AND NOT EXISTS (SELECT ra FROM RoomAvailabilityEntity ra WHERE ra.room = r " +
           "    AND ra.date >= :checkIn AND ra.date < :checkOut AND ra.status IN ('booked', 'blocked')) " +
           "AND NOT EXISTS (SELECT b FROM HotelBookingEntity b WHERE b.room = r " +
           "    AND b.status = 'pending' AND b.createdAt >= :holdSince " +
           "    AND b.checkInDate < :checkOut AND b.checkOutDate > :checkIn " +
           "    AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)) " +
           "ORDER BY r.roomNumber")
    List<RoomEntity> findBookableRooms(@Param("roomTypeId") UUID roomTypeId,
                                       @Param("checkIn") LocalDate checkIn,
                                       @Param("checkOut") LocalDate checkOut,
                                       @Param("holdSince") LocalDateTime holdSince,
                                       @Param("excludeBookingId") UUID excludeBookingId);

    /** Khoá dòng phòng để tuần tự hoá việc xác nhận booking cho cùng 1 phòng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r WHERE r.id = :roomId")
    Optional<RoomEntity> lockById(@Param("roomId") UUID roomId);

    @Query("SELECT rta.roomType.id, rta.amenity.name FROM RoomTypeAmenityEntity rta WHERE rta.roomType.id IN :roomTypeIds")
    List<Object[]> findAmenityNamesByRoomTypeIds(@Param("roomTypeIds") List<UUID> roomTypeIds);
}
