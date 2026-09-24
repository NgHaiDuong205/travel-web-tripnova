package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.HotelBookingEntity;
import com.duong.travelweb.repository.custom.HotelBookingRepositoryCustom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelBookingRepository extends JpaRepository<HotelBookingEntity, UUID>, HotelBookingRepositoryCustom {

    @Query("SELECT b FROM HotelBookingEntity b " +
           "JOIN FETCH b.order JOIN FETCH b.hotel JOIN FETCH b.roomType LEFT JOIN FETCH b.room " +
           "WHERE b.id = :bookingId")
    Optional<HotelBookingEntity> findDetailById(@Param("bookingId") UUID bookingId);

    @Query("SELECT b FROM HotelBookingEntity b JOIN FETCH b.hotel JOIN FETCH b.roomType LEFT JOIN FETCH b.room " +
           "WHERE b.order.id = :orderId")
    List<HotelBookingEntity> findByOrderId(@Param("orderId") UUID orderId);

    /** [orderId, bookingId] — map đơn hàng sang booking theo lô. */
    @Query("SELECT b.order.id, b.id FROM HotelBookingEntity b WHERE b.order.id IN :orderIds")
    List<Object[]> findBookingIdsByOrderIds(@Param("orderIds") List<UUID> orderIds);

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM HotelBookingEntity b WHERE b.user.id = :userId " +
           "AND b.status IN ('confirmed', 'checked_in', 'checked_out', 'completed')")
    BigDecimal sumSpentByUser(@Param("userId") UUID userId);

    /** Chuyến sắp tới gần nhất đã xác nhận. */
    @Query("SELECT b FROM HotelBookingEntity b JOIN FETCH b.order JOIN FETCH b.hotel JOIN FETCH b.roomType LEFT JOIN FETCH b.room " +
           "WHERE b.user.id = :userId AND b.status IN ('confirmed', 'checked_in') AND b.checkOutDate >= :today " +
           "ORDER BY b.checkInDate ASC")
    List<HotelBookingEntity> findUpcomingConfirmed(@Param("userId") UUID userId,
                                                   @Param("today") LocalDate today,
                                                   Pageable pageable);

    /** [status, số booking] cho dashboard admin. */
    @Query("SELECT b.status, COUNT(b) FROM HotelBookingEntity b GROUP BY b.status")
    List<Object[]> countGroupByStatus();

    /** Booking pending quá hạn giữ phòng — dùng cho job tự huỷ. */
    @Query("SELECT b FROM HotelBookingEntity b WHERE b.status = 'pending' AND b.createdAt < :before")
    List<HotelBookingEntity> findPendingCreatedBefore(@Param("before") LocalDateTime before);

    @Query("SELECT COUNT(b) > 0 FROM HotelBookingEntity b WHERE b.room.id = :roomId")
    boolean existsByRoomId(@Param("roomId") UUID roomId);

    /** Phòng còn booking chưa kết thúc (pending/confirmed/checked_in, chưa tới ngày trả phòng). */
    @Query("SELECT COUNT(b) > 0 FROM HotelBookingEntity b WHERE b.room.id = :roomId " +
           "AND b.status IN ('pending', 'confirmed', 'checked_in') AND b.checkOutDate > :today")
    boolean existsOpenBookingForRoom(@Param("roomId") UUID roomId, @Param("today") LocalDate today);

    /** Booking còn mở của phòng giao với khoảng ngày [from, toExclusive). */
    @Query("SELECT COUNT(b) > 0 FROM HotelBookingEntity b WHERE b.room.id = :roomId " +
           "AND b.status IN ('pending', 'confirmed', 'checked_in') " +
           "AND b.checkInDate < :toExclusive AND b.checkOutDate > :from")
    boolean existsOpenBookingInRange(@Param("roomId") UUID roomId,
                                     @Param("from") LocalDate from,
                                     @Param("toExclusive") LocalDate toExclusive);
}
