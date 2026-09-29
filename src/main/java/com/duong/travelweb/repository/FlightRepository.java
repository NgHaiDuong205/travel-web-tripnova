package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.FlightEntity;
import com.duong.travelweb.repository.custom.FlightRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FlightRepository extends JpaRepository<FlightEntity, UUID>, FlightRepositoryCustom {

    /** Đếm lại total_seats / available_seats từ flight_seats (ghế blocked không tính là trống). */
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE flights SET " +
                   "total_seats = (SELECT COUNT(*) FROM flight_seats s WHERE s.flight_id = :flightId), " +
                   "available_seats = (SELECT COUNT(*) FROM flight_seats s WHERE s.flight_id = :flightId AND s.status = 'available') " +
                   "WHERE id = :flightId", nativeQuery = true)
    void syncSeatCounts(@Param("flightId") UUID flightId);

    /** [mã sân bay, thành phố] của các chuyến đang bán (cả điểm đi và điểm đến). */
    @Query(value = "SELECT code, MAX(city) FROM (" +
                   "SELECT departure_airport_code AS code, departure_city AS city FROM flights WHERE is_active AND departure_time > :now " +
                   "UNION ALL SELECT arrival_airport_code, arrival_city FROM flights WHERE is_active AND departure_time > :now) a " +
                   "GROUP BY code ORDER BY code", nativeQuery = true)
    List<Object[]> findAirports(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT f.airline FROM FlightEntity f WHERE f.isActive = true AND f.departureTime > :now ORDER BY f.airline")
    List<String> findActiveAirlines(@Param("now") LocalDateTime now);

    /** [giá ghế trống thấp nhất, cao nhất] của các chuyến đang bán. */
    @Query("SELECT MIN(s.price), MAX(s.price) FROM FlightSeatEntity s WHERE s.status = 'available' " +
           "AND s.flight.isActive = true AND s.flight.departureTime > :now")
    List<Object[]> findActivePriceRange(@Param("now") LocalDateTime now);
}
