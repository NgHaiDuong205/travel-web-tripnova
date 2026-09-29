package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.TourBookingEntity;
import com.duong.travelweb.repository.custom.TourBookingRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourBookingRepository extends JpaRepository<TourBookingEntity, UUID>, TourBookingRepositoryCustom {

    @Query("SELECT COUNT(b) > 0 FROM TourBookingEntity b WHERE b.order.id = :orderId")
    boolean existsByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT b FROM TourBookingEntity b JOIN FETCH b.tour WHERE b.order.id = :orderId ORDER BY b.createdAt, b.id")
    List<TourBookingEntity> findByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT b FROM TourBookingEntity b JOIN FETCH b.tour t LEFT JOIN FETCH t.destination JOIN FETCH b.order JOIN FETCH b.user " +
           "WHERE b.id = :id")
    Optional<TourBookingEntity> findDetailById(@Param("id") UUID id);

    /**
     * Số khách (người lớn + trẻ em) đã giữ chỗ của tour trong ngày khởi hành: confirmed / checked_in / completed,
     * hoặc pending còn trong hạn giữ chỗ.
     * @param excludeId đơn đang xét (null = không loại trừ)
     */
    @Query("SELECT COALESCE(SUM(b.numAdults + COALESCE(b.numChildren, 0)), 0) FROM TourBookingEntity b " +
           "WHERE b.tour.id = :tourId AND b.departureDate = :departureDate AND (:excludeId IS NULL OR b.id <> :excludeId) " +
           "AND (b.status IN ('confirmed', 'checked_in', 'completed') OR (b.status = 'pending' AND b.createdAt >= :holdCutoff))")
    long countBookedGuests(@Param("tourId") UUID tourId, @Param("departureDate") LocalDate departureDate,
                           @Param("excludeId") UUID excludeId, @Param("holdCutoff") LocalDateTime holdCutoff);

    @Query("SELECT b FROM TourBookingEntity b WHERE b.status = 'pending' AND b.createdAt < :before")
    List<TourBookingEntity> findPendingCreatedBefore(@Param("before") LocalDateTime before);

    @Query(value = "SELECT b FROM TourBookingEntity b JOIN FETCH b.tour t LEFT JOIN FETCH t.destination JOIN FETCH b.order " +
                   "WHERE b.user.id = :userId ORDER BY b.departureDate DESC",
           countQuery = "SELECT COUNT(b) FROM TourBookingEntity b WHERE b.user.id = :userId")
    Page<TourBookingEntity> findByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT b FROM TourBookingEntity b JOIN FETCH b.tour t LEFT JOIN FETCH t.destination JOIN FETCH b.order " +
                   "WHERE b.user.id = :userId AND b.status IN :statuses ORDER BY b.departureDate DESC",
           countQuery = "SELECT COUNT(b) FROM TourBookingEntity b WHERE b.user.id = :userId AND b.status IN :statuses")
    Page<TourBookingEntity> findByUserAndStatuses(@Param("userId") UUID userId,
                                                  @Param("statuses") Collection<String> statuses, Pageable pageable);

    /** [orderId, tổng tiền đã hoàn] — đơn refunded = hoàn đủ total_price (bảng không có cột hoàn tiền). */
    @Query("SELECT b.order.id, COALESCE(SUM(b.totalPrice), 0) FROM TourBookingEntity b " +
           "WHERE b.order.id IN :orderIds AND b.status = 'refunded' GROUP BY b.order.id")
    List<Object[]> sumRefundByOrderIds(@Param("orderIds") Collection<UUID> orderIds);
}
