package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.HotelBookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelBookingRepository extends JpaRepository<HotelBookingEntity, UUID> {

    @Query("SELECT b FROM HotelBookingEntity b " +
           "JOIN FETCH b.order JOIN FETCH b.hotel JOIN FETCH b.roomType LEFT JOIN FETCH b.room " +
           "WHERE b.id = :bookingId")
    Optional<HotelBookingEntity> findDetailById(@Param("bookingId") UUID bookingId);

    @Query("SELECT b FROM HotelBookingEntity b JOIN FETCH b.hotel JOIN FETCH b.roomType LEFT JOIN FETCH b.room " +
           "WHERE b.order.id = :orderId")
    List<HotelBookingEntity> findByOrderId(@Param("orderId") UUID orderId);

    /** Booking pending quá hạn giữ phòng — dùng cho job tự huỷ. */
    @Query("SELECT b FROM HotelBookingEntity b WHERE b.status = 'pending' AND b.createdAt < :before")
    List<HotelBookingEntity> findPendingCreatedBefore(@Param("before") LocalDateTime before);
}
