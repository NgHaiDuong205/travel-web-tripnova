package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.FlightSeatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlightSeatRepository extends JpaRepository<FlightSeatEntity, UUID> {

    /** Sơ đồ ghế, sắp theo số hàng rồi chữ cái (1A, 1B, ..., 10A). */
    @Query(value = "SELECT * FROM flight_seats WHERE flight_id = :flightId " +
                   "ORDER BY NULLIF(regexp_replace(seat_number, '[^0-9]', '', 'g'), '')::int NULLS LAST, seat_number",
           nativeQuery = true)
    List<FlightSeatEntity> findByFlightOrdered(@Param("flightId") UUID flightId);

    /** Khoá các ghế theo thứ tự id (tránh deadlock khi hai người cùng chọn nhiều ghế). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM FlightSeatEntity s JOIN FETCH s.flight WHERE s.id IN :ids ORDER BY s.id")
    List<FlightSeatEntity> lockByIds(@Param("ids") Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM FlightSeatEntity s WHERE s.id = :id")
    Optional<FlightSeatEntity> lockById(@Param("id") UUID id);

    @Query("SELECT COUNT(s) > 0 FROM FlightSeatEntity s WHERE s.flight.id = :flightId AND UPPER(s.seatNumber) = :seatNumber " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    boolean existsSeatNumber(@Param("flightId") UUID flightId, @Param("seatNumber") String seatNumber,
                             @Param("excludeId") UUID excludeId);

    /** [flightId, hạng, giá thấp nhất, số ghế] của ghế còn trống. */
    @Query("SELECT s.flight.id, s.seatClass, MIN(s.price), COUNT(s) FROM FlightSeatEntity s " +
           "WHERE s.flight.id IN :flightIds AND s.status = 'available' GROUP BY s.flight.id, s.seatClass")
    List<Object[]> summarizeAvailable(@Param("flightIds") Collection<UUID> flightIds);

    /** Xoá các ghế chưa từng gắn với vé nào (dùng khi sinh lại sơ đồ). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM flight_seats s WHERE s.flight_id = :flightId " +
                   "AND NOT EXISTS (SELECT 1 FROM flight_bookings b WHERE b.seat_id = s.id)", nativeQuery = true)
    int deleteUnusedSeats(@Param("flightId") UUID flightId);
}
