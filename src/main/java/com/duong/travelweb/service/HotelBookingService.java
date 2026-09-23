package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.HotelBookingCreatedDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.HotelBookingRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityCheckDTO;
import com.duong.travelweb.model.entity.OrderEntity;

import java.time.LocalDate;
import java.util.UUID;

public interface HotelBookingService {
    RoomAvailabilityCheckDTO checkAvailability(UUID hotelId, UUID roomTypeId, UUID roomId, LocalDate checkIn, LocalDate checkOut);
    HotelBookingCreatedDTO createBooking(UUID userId, HotelBookingRequestDTO request);
    HotelBookingDTO getBooking(UUID userId, UUID bookingId);
    HotelBookingDTO cancelBooking(UUID userId, UUID bookingId, String reason);

    /** Gọi khi cổng thanh toán báo thành công. Trả về false nếu phòng đã hết và đơn phải hoàn tiền. */
    boolean confirmOrder(OrderEntity order);

    /** Gọi khi thanh toán thất bại / hết hạn giữ phòng. */
    void cancelPendingOrder(OrderEntity order, String reason);

    /** Huỷ các booking pending quá hạn giữ phòng, trả về số booking đã huỷ. */
    int expirePendingBookings();
}
