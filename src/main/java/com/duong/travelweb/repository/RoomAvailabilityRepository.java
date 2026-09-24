package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RoomAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailabilityEntity, UUID> {

    @Query("SELECT COUNT(ra) FROM RoomAvailabilityEntity ra WHERE ra.room.id = :roomId " +
           "AND ra.date >= :checkIn AND ra.date < :checkOut AND ra.status IN ('booked', 'blocked')")
    long countUnavailableDays(@Param("roomId") UUID roomId,
                              @Param("checkIn") LocalDate checkIn,
                              @Param("checkOut") LocalDate checkOut);

    /** Khoá 1 ngày của phòng. Dòng 'available' có sẵn thì chuyển sang 'booked'. Trả về số dòng bị ảnh hưởng. */
    @Modifying
    @Query(value = "INSERT INTO room_availability (id, room_id, date, status) " +
                   "VALUES (gen_random_uuid(), :roomId, :date, 'booked') " +
                   "ON CONFLICT (room_id, date) DO UPDATE SET status = 'booked', blocked_reason = NULL " +
                   "WHERE room_availability.status = 'available'",
           nativeQuery = true)
    int bookDay(@Param("roomId") UUID roomId, @Param("date") LocalDate date);

    /** Trả phòng về trạng thái trống khi huỷ booking đã xác nhận. */
    @Modifying
    @Query(value = "DELETE FROM room_availability WHERE room_id = :roomId " +
                   "AND date >= :checkIn AND date < :checkOut AND status = 'booked'",
           nativeQuery = true)
    int releaseDays(@Param("roomId") UUID roomId,
                    @Param("checkIn") LocalDate checkIn,
                    @Param("checkOut") LocalDate checkOut);

    @Query("SELECT ra FROM RoomAvailabilityEntity ra WHERE ra.room.id = :roomId " +
           "AND ra.date >= :from AND ra.date <= :to AND ra.status <> 'available' ORDER BY ra.date")
    List<RoomAvailabilityEntity> findUnavailableInRange(@Param("roomId") UUID roomId,
                                                        @Param("from") LocalDate from,
                                                        @Param("to") LocalDate to);

    @Query("SELECT COUNT(ra) > 0 FROM RoomAvailabilityEntity ra WHERE ra.room.id = :roomId AND ra.status = 'booked'")
    boolean existsBookedDay(@Param("roomId") UUID roomId);

    @Query("SELECT COUNT(ra) FROM RoomAvailabilityEntity ra WHERE ra.room.id = :roomId " +
           "AND ra.date >= :from AND ra.date <= :to AND ra.status = 'booked'")
    long countBookedInRange(@Param("roomId") UUID roomId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Admin khoá 1 ngày (không đè lên ngày đã booked). */
    @Modifying
    @Query(value = "INSERT INTO room_availability (id, room_id, date, status, blocked_reason) " +
                   "VALUES (gen_random_uuid(), :roomId, :date, 'blocked', CAST(:reason AS varchar)) " +
                   "ON CONFLICT (room_id, date) DO UPDATE SET status = 'blocked', blocked_reason = EXCLUDED.blocked_reason " +
                   "WHERE room_availability.status <> 'booked'",
           nativeQuery = true)
    int blockDay(@Param("roomId") UUID roomId, @Param("date") LocalDate date, @Param("reason") String reason);

    /** Admin mở khoá các ngày blocked trong [from, to]. */
    @Modifying
    @Query(value = "DELETE FROM room_availability WHERE room_id = :roomId " +
                   "AND date >= :from AND date <= :to AND status IN ('blocked', 'available')",
           nativeQuery = true)
    int unblockDays(@Param("roomId") UUID roomId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
