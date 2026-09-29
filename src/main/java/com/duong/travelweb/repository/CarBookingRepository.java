package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CarBookingEntity;
import com.duong.travelweb.repository.custom.CarBookingRepositoryCustom;
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

public interface CarBookingRepository extends JpaRepository<CarBookingEntity, UUID>, CarBookingRepositoryCustom {

    @Query("SELECT COUNT(b) > 0 FROM CarBookingEntity b WHERE b.order.id = :orderId")
    boolean existsByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT b FROM CarBookingEntity b JOIN FETCH b.car WHERE b.order.id = :orderId ORDER BY b.pickupDate, b.id")
    List<CarBookingEntity> findByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT b FROM CarBookingEntity b JOIN FETCH b.car JOIN FETCH b.order JOIN FETCH b.user WHERE b.id = :id")
    Optional<CarBookingEntity> findDetailById(@Param("id") UUID id);

    /**
     * Booking khác đang chiếm xe và giao với [from, to): confirmed / checked_in, hoặc pending còn trong hạn giữ chỗ.
     * @param excludeId booking đang xét (null = không loại trừ)
     */
    @Query("SELECT b FROM CarBookingEntity b WHERE b.car.id = :carId AND (:excludeId IS NULL OR b.id <> :excludeId) " +
           "AND b.pickupDate < :to AND b.returnDate > :from " +
           "AND (b.status IN ('confirmed', 'checked_in') OR (b.status = 'pending' AND b.createdAt >= :holdCutoff)) " +
           "ORDER BY b.pickupDate")
    List<CarBookingEntity> findBlocking(@Param("carId") UUID carId, @Param("excludeId") UUID excludeId,
                                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                        @Param("holdCutoff") LocalDateTime holdCutoff);

    @Query("SELECT b FROM CarBookingEntity b WHERE b.status = 'pending' AND b.createdAt < :before")
    List<CarBookingEntity> findPendingCreatedBefore(@Param("before") LocalDateTime before);

    @Query(value = "SELECT b FROM CarBookingEntity b JOIN FETCH b.car JOIN FETCH b.order WHERE b.user.id = :userId " +
                   "ORDER BY b.pickupDate DESC",
           countQuery = "SELECT COUNT(b) FROM CarBookingEntity b WHERE b.user.id = :userId")
    Page<CarBookingEntity> findByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT b FROM CarBookingEntity b JOIN FETCH b.car JOIN FETCH b.order WHERE b.user.id = :userId " +
                   "AND b.status IN :statuses ORDER BY b.pickupDate DESC",
           countQuery = "SELECT COUNT(b) FROM CarBookingEntity b WHERE b.user.id = :userId AND b.status IN :statuses")
    Page<CarBookingEntity> findByUserAndStatuses(@Param("userId") UUID userId,
                                                 @Param("statuses") Collection<String> statuses, Pageable pageable);

    /** [orderId, tổng tiền đã hoàn] — xe không có cột refund_amount: booking refunded = hoàn đủ total_price. */
    @Query("SELECT b.order.id, COALESCE(SUM(b.totalPrice), 0) FROM CarBookingEntity b " +
           "WHERE b.order.id IN :orderIds AND b.status = 'refunded' GROUP BY b.order.id")
    List<Object[]> sumRefundByOrderIds(@Param("orderIds") Collection<UUID> orderIds);

    /** Xe còn booking chưa kết thúc (không cho xoá hẳn / tắt khi còn khách). */
    @Query("SELECT COUNT(b) > 0 FROM CarBookingEntity b WHERE b.car.id = :carId " +
           "AND b.status IN ('pending', 'confirmed', 'checked_in') AND b.returnDate > :now")
    boolean existsOpenByCar(@Param("carId") UUID carId, @Param("now") LocalDateTime now);
}
