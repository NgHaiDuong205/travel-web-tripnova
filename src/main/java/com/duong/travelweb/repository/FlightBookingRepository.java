package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.FlightBookingEntity;
import com.duong.travelweb.repository.custom.FlightBookingRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlightBookingRepository extends JpaRepository<FlightBookingEntity, UUID>, FlightBookingRepositoryCustom {

    @Query("SELECT COUNT(b) > 0 FROM FlightBookingEntity b WHERE b.order.id = :orderId")
    boolean existsByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT b FROM FlightBookingEntity b JOIN FETCH b.flight LEFT JOIN FETCH b.seat " +
           "WHERE b.order.id = :orderId ORDER BY b.createdAt, b.id")
    List<FlightBookingEntity> findByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT b FROM FlightBookingEntity b JOIN FETCH b.flight LEFT JOIN FETCH b.seat JOIN FETCH b.order JOIN FETCH b.user " +
           "WHERE b.id = :id")
    Optional<FlightBookingEntity> findDetailById(@Param("id") UUID id);

    /** Vé khác đang giữ ghế này (pending / confirmed / checked_in / completed). */
    @Query("SELECT COUNT(b) > 0 FROM FlightBookingEntity b WHERE b.seat.id = :seatId AND b.id <> :bookingId " +
           "AND b.status IN ('pending', 'confirmed', 'checked_in', 'completed')")
    boolean seatTakenByOther(@Param("seatId") UUID seatId, @Param("bookingId") UUID bookingId);

    /** Ghế từng gắn với vé nào chưa (kể cả vé đã huỷ / hoàn) — FK seat_id là ON DELETE SET NULL nên phải tự chặn xoá. */
    @Query("SELECT COUNT(b) > 0 FROM FlightBookingEntity b WHERE b.seat.id = :seatId")
    boolean existsBySeatId(@Param("seatId") UUID seatId);

    @Query("SELECT b FROM FlightBookingEntity b WHERE b.status = 'pending' AND b.createdAt < :before")
    List<FlightBookingEntity> findPendingCreatedBefore(@Param("before") LocalDateTime before);

    @Query(value = "SELECT b FROM FlightBookingEntity b JOIN FETCH b.flight LEFT JOIN FETCH b.seat JOIN FETCH b.order " +
                   "WHERE b.user.id = :userId ORDER BY b.flight.departureTime DESC, b.createdAt",
           countQuery = "SELECT COUNT(b) FROM FlightBookingEntity b WHERE b.user.id = :userId")
    Page<FlightBookingEntity> findByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT b FROM FlightBookingEntity b JOIN FETCH b.flight LEFT JOIN FETCH b.seat JOIN FETCH b.order " +
                   "WHERE b.user.id = :userId AND b.status IN :statuses ORDER BY b.flight.departureTime DESC, b.createdAt",
           countQuery = "SELECT COUNT(b) FROM FlightBookingEntity b WHERE b.user.id = :userId AND b.status IN :statuses")
    Page<FlightBookingEntity> findByUserAndStatuses(@Param("userId") UUID userId,
                                                    @Param("statuses") Collection<String> statuses, Pageable pageable);

    /** [orderId, tổng tiền đã hoàn] — vé refunded = hoàn đủ total_price (bảng không có cột hoàn tiền). */
    @Query("SELECT b.order.id, COALESCE(SUM(b.totalPrice), 0) FROM FlightBookingEntity b " +
           "WHERE b.order.id IN :orderIds AND b.status = 'refunded' GROUP BY b.order.id")
    List<Object[]> sumRefundByOrderIds(@Param("orderIds") Collection<UUID> orderIds);

    @Query("SELECT COUNT(b) > 0 FROM FlightBookingEntity b WHERE b.flight.id = :flightId " +
           "AND b.status IN ('pending', 'confirmed', 'checked_in')")
    boolean existsOpenByFlight(@Param("flightId") UUID flightId);
}
