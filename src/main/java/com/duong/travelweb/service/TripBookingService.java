package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.TripBookingDTO;
import com.duong.travelweb.model.dto.TripBookingQuoteDTO;
import com.duong.travelweb.model.dto.TripBookingRequestDTO;

import java.util.UUID;

/**
 * Đặt trọn gói từ lịch trình của tôi: khách sạn (nhiều phòng) + thuê xe trong một lần, thanh toán một lần.
 * Mỗi loại là một order riêng (kiến trúc 1 order = 1 loại booking), tạo trong cùng transaction và gắn với lịch trình
 * qua orders.notes ("itinerary:<id> batch:<id>").
 */
public interface TripBookingService {
    /** hotelId null = khách sạn trong lịch trình. */
    TripBookingQuoteDTO quote(UUID userId, UUID itineraryId, UUID hotelId);

    /** Tạo các order pending (giữ chỗ theo app.booking.hold-minutes). 409 nếu lịch trình đang có lượt đặt còn hiệu lực. */
    TripBookingDTO book(UUID userId, UUID itineraryId, TripBookingRequestDTO request);

    /** Lượt đặt gần nhất của lịch trình (state = none nếu chưa đặt). */
    TripBookingDTO get(UUID userId, UUID itineraryId);

    /** Thanh toán (cổng giả lập) mọi order đang chờ của lượt đặt; thành công hết → lịch trình "booked". */
    TripBookingDTO pay(UUID userId, UUID itineraryId, boolean success);

    /** Huỷ các order chưa thanh toán của lượt đặt (nhả phòng / xe ngay). */
    TripBookingDTO cancel(UUID userId, UUID itineraryId);
}
