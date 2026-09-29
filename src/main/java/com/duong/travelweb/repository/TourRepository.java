package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.TourEntity;
import com.duong.travelweb.repository.custom.TourRepositoryCustom;
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

/**
 * tour_hotels / tour_cars / tour_flights (khoá chính ghép, không có id) được đọc / ghi bằng native query,
 * không qua entity @IdClass (saveAll / merge với @IdClass từng sinh SQL lỗi — xem room_type_amenities).
 */
public interface TourRepository extends JpaRepository<TourEntity, UUID>, TourRepositoryCustom {

    @Query("SELECT t FROM TourEntity t LEFT JOIN FETCH t.destination d LEFT JOIN FETCH d.country WHERE t.id = :id")
    Optional<TourEntity> findDetailById(@Param("id") UUID id);

    /** Khoá tour khi đặt / xác nhận để không bán quá số chỗ của một ngày khởi hành. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TourEntity t WHERE t.id = :id")
    Optional<TourEntity> lockById(@Param("id") UUID id);

    @Query("SELECT COUNT(t) > 0 FROM TourEntity t WHERE t.slug = :slug AND (:excludeId IS NULL OR t.id <> :excludeId)")
    boolean existsBySlug(@Param("slug") String slug, @Param("excludeId") UUID excludeId);

    // ---- tour_hotels ----

    /** [hotel_id, check_in_day, nights] */
    @Query(value = "SELECT hotel_id, check_in_day, nights FROM tour_hotels WHERE tour_id = :tourId " +
                   "ORDER BY check_in_day NULLS LAST", nativeQuery = true)
    List<Object[]> findHotelLinks(@Param("tourId") UUID tourId);

    /** [tour_id, số khách sạn] */
    @Query(value = "SELECT tour_id, COUNT(*) FROM tour_hotels WHERE tour_id IN :tourIds GROUP BY tour_id", nativeQuery = true)
    List<Object[]> countHotels(@Param("tourIds") Collection<UUID> tourIds);

    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO tour_hotels (tour_id, hotel_id, check_in_day, nights) VALUES (:tourId, :hotelId, :checkInDay, :nights) " +
                   "ON CONFLICT (tour_id, hotel_id) DO UPDATE SET check_in_day = EXCLUDED.check_in_day, nights = EXCLUDED.nights",
           nativeQuery = true)
    void upsertHotel(@Param("tourId") UUID tourId, @Param("hotelId") UUID hotelId,
                     @Param("checkInDay") Short checkInDay, @Param("nights") Short nights);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM tour_hotels WHERE tour_id = :tourId AND hotel_id = :hotelId", nativeQuery = true)
    int deleteHotel(@Param("tourId") UUID tourId, @Param("hotelId") UUID hotelId);

    // ---- tour_cars ----

    /** [car_id, usage_day] */
    @Query(value = "SELECT car_id, usage_day FROM tour_cars WHERE tour_id = :tourId ORDER BY usage_day NULLS LAST", nativeQuery = true)
    List<Object[]> findCarLinks(@Param("tourId") UUID tourId);

    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO tour_cars (tour_id, car_id, usage_day) VALUES (:tourId, :carId, :usageDay) " +
                   "ON CONFLICT (tour_id, car_id) DO UPDATE SET usage_day = EXCLUDED.usage_day", nativeQuery = true)
    void upsertCar(@Param("tourId") UUID tourId, @Param("carId") UUID carId, @Param("usageDay") Short usageDay);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM tour_cars WHERE tour_id = :tourId AND car_id = :carId", nativeQuery = true)
    int deleteCar(@Param("tourId") UUID tourId, @Param("carId") UUID carId);

    // ---- tour_flights ----

    /** [flight_id, leg] */
    @Query(value = "SELECT flight_id, leg FROM tour_flights WHERE tour_id = :tourId", nativeQuery = true)
    List<Object[]> findFlightLinks(@Param("tourId") UUID tourId);

    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO tour_flights (tour_id, flight_id, leg) VALUES (:tourId, :flightId, :leg) " +
                   "ON CONFLICT (tour_id, flight_id) DO UPDATE SET leg = EXCLUDED.leg", nativeQuery = true)
    void upsertFlight(@Param("tourId") UUID tourId, @Param("flightId") UUID flightId, @Param("leg") String leg);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM tour_flights WHERE tour_id = :tourId AND flight_id = :flightId", nativeQuery = true)
    int deleteFlight(@Param("tourId") UUID tourId, @Param("flightId") UUID flightId);
}
