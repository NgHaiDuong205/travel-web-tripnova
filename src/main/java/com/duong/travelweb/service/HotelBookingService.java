package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.HotelBookingCreatedDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.HotelBookingRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityCheckDTO;
import com.duong.travelweb.model.entity.OrderEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HotelBookingService {
    RoomAvailabilityCheckDTO checkAvailability(UUID hotelId, UUID roomTypeId, UUID roomId, LocalDate checkIn, LocalDate checkOut);
    HotelBookingCreatedDTO createBooking(UUID userId, HotelBookingRequestDTO request);
    HotelBookingDTO getBooking(UUID userId, UUID bookingId);
    HotelBookingDTO cancelBooking(UUID userId, UUID bookingId, String reason);

    /** @param statusGroup all | upcoming | completed | cancelled | pending */
    List<HotelBookingDTO> findMyBookings(UUID userId, String statusGroup, int page, int limit);
    long countMyBookings(UUID userId, String statusGroup);

    // ---- Admin ----
    List<HotelBookingDTO> findForAdmin(String status, String keyword, int page, int limit);
    long countForAdmin(String status, String keyword);

    /** Chuyển trạng thái: confirmed→checked_in→checked_out→completed, confirmed→no_show, pending|confirmed→cancelled. */
    HotelBookingDTO updateStatusByAdmin(UUID bookingId, String newStatus, String reason);

    /** Chuyến sắp tới gần nhất đã xác nhận, null nếu không có. */
    HotelBookingDTO findNextUpcoming(UUID userId);

    /** Gọi khi cổng thanh toán báo thành công. Trả về false nếu phòng đã hết và đơn phải hoàn tiền. */
    boolean confirmOrder(OrderEntity order);

    /** Gọi khi thanh toán thất bại / hết hạn giữ phòng. */
    void cancelPendingOrder(OrderEntity order, String reason);

    /** Huỷ các booking pending quá hạn giữ phòng, trả về số booking đã huỷ. */
    int expirePendingBookings();
}
