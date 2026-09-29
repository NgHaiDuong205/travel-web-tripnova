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

    /** [orderId, tổng tiền đã hoàn] */
    @Query("SELECT b.order.id, COALESCE(SUM(b.refundAmount), 0) FROM HotelBookingEntity b " +
           "WHERE b.order.id IN :orderIds GROUP BY b.order.id")
    List<Object[]> sumRefundByOrderIds(@Param("orderIds") List<UUID> orderIds);

    /** User đã thực sự ở khách sạn (đã trả phòng, hoặc booking đã xác nhận mà ngày trả phòng đã qua). */
    @Query("SELECT COUNT(b) > 0 FROM HotelBookingEntity b WHERE b.user.id = :userId AND b.hotel.id = :hotelId " +
           "AND (b.status IN ('checked_out', 'completed') " +
           "OR (b.status IN ('confirmed', 'checked_in') AND b.checkOutDate <= :today))")
    boolean existsCompletedStay(@Param("userId") UUID userId, @Param("hotelId") UUID hotelId,
                                @Param("today") LocalDate today);

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

    // ---- Thống kê dashboard (theo created_at trong [from, to)) ----

    @Query("SELECT b.status, COUNT(b) FROM HotelBookingEntity b WHERE b.createdAt >= :from AND b.createdAt < :to GROUP BY b.status")
    List<Object[]> countGroupByStatusBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Giá trị trung bình của booking đã được thanh toán (kể cả đã hoàn một phần). */
    @Query("SELECT COALESCE(AVG(b.totalPrice), 0) FROM HotelBookingEntity b WHERE b.createdAt >= :from AND b.createdAt < :to " +
           "AND b.status IN ('confirmed', 'checked_in', 'checked_out', 'completed', 'no_show', 'refunded')")
    BigDecimal averagePaidBookingValueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(b.refundAmount), 0) FROM HotelBookingEntity b WHERE b.refundedAt >= :from AND b.refundedAt < :to")
    BigDecimal sumRefundedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** [ngày đầu kỳ, số booking]; unit = day | week | month. */
    @Query(value = "SELECT CAST(date_trunc(CAST(:unit AS text), created_at) AS date) AS bucket, COUNT(*) FROM hotel_bookings " +
                   "WHERE created_at >= :from AND created_at < :to GROUP BY 1 ORDER BY 1",
           nativeQuery = true)
    List<Object[]> countByBucket(@Param("unit") String unit, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Top khách sạn theo doanh thu ròng (total_price - refund_amount) của booking đã thanh toán và chưa hoàn đủ.
     * [hotel_id, tên, tên điểm đến, số booking, số đêm, doanh thu]
     */
    @Query(value = "SELECT h.id, h.name, d.name AS destination, COUNT(b.id), COALESCE(SUM(b.num_nights), 0), " +
                   "SUM(b.total_price - COALESCE(b.refund_amount, 0)) AS revenue " +
                   "FROM hotel_bookings b JOIN hotels h ON h.id = b.hotel_id LEFT JOIN destinations d ON d.id = h.destination_id " +
                   "WHERE b.status IN ('confirmed', 'checked_in', 'checked_out', 'completed', 'no_show') " +
                   "AND b.created_at >= :from AND b.created_at < :to " +
                   "GROUP BY h.id, h.name, d.name ORDER BY revenue DESC, COUNT(b.id) DESC LIMIT :limit",
           nativeQuery = true)
    List<Object[]> topHotels(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("limit") int limit);

    /** Như topHotels nhưng gom theo điểm đến: [destination_id, tên, tên quốc gia, số booking, số đêm, doanh thu]. */
    @Query(value = "SELECT d.id, d.name, c.name AS country, COUNT(b.id), COALESCE(SUM(b.num_nights), 0), " +
                   "SUM(b.total_price - COALESCE(b.refund_amount, 0)) AS revenue " +
                   "FROM hotel_bookings b JOIN hotels h ON h.id = b.hotel_id JOIN destinations d ON d.id = h.destination_id " +
                   "LEFT JOIN countries c ON c.id = d.country_id " +
                   "WHERE b.status IN ('confirmed', 'checked_in', 'checked_out', 'completed', 'no_show') " +
                   "AND b.created_at >= :from AND b.created_at < :to " +
                   "GROUP BY d.id, d.name, c.name ORDER BY revenue DESC, COUNT(b.id) DESC LIMIT :limit",
           nativeQuery = true)
    List<Object[]> topDestinations(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("limit") int limit);

    /** User còn booking chưa kết thúc (pending, hoặc confirmed/checked_in chưa qua ngày trả phòng). */
    @Query("SELECT COUNT(b) > 0 FROM HotelBookingEntity b WHERE b.user.id = :userId " +
           "AND (b.status = 'pending' OR (b.status IN ('confirmed', 'checked_in') AND b.checkOutDate >= :today))")
    boolean existsOpenBookingForUser(@Param("userId") UUID userId, @Param("today") LocalDate today);

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
