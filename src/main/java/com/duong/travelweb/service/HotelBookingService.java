package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.HotelBookingCreatedDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.HotelBookingRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityCheckDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HotelBookingService extends OrderBookingHandler {
    RoomAvailabilityCheckDTO checkAvailability(UUID hotelId, UUID roomTypeId, UUID roomId, LocalDate checkIn, LocalDate checkOut);
    HotelBookingCreatedDTO createBooking(UUID userId, HotelBookingRequestDTO request);

    /** Số phòng đặt được của loại phòng trong [checkIn, checkOut) — không kiểm tra hợp lệ, không ném lỗi. */
    int countBookableRooms(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut);

    /**
     * Một dòng đặt nhiều phòng cùng loại, cùng ngày. adults/children là TỔNG cho cả dòng,
     * được chia đều vào từng phòng (mỗi phòng ≥ 1 người lớn).
     */
    record HotelBookingLine(UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut,
                            int quantity, int adults, int children, String specialRequests) {
    }

    /**
     * Tạo 1 order (pending) gồm nhiều booking + 1 payment, giữ phòng như createBooking.
     * Nguyên tử: thiếu phòng ở bất kỳ dòng nào -> 409, không tạo gì.
     */
    CartCheckoutDTO createOrder(UUID userId, List<HotelBookingLine> lines, String paymentMethod);
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

    /**
     * Admin hoàn tiền (bỏ qua hạn huỷ miễn phí). Áp dụng cho confirmed | no_show | checked_out | completed.
     * @param amount null = hoàn toàn bộ; 0 < amount <= totalPrice
     */
    HotelBookingDTO refundByAdmin(UUID bookingId, BigDecimal amount, String reason);


    /** Xoá hẳn booking đã huỷ và chưa từng thanh toán (kèm order/payment nếu order không còn booking nào). */
    void deleteByAdmin(UUID bookingId);

    /** Chuyến sắp tới gần nhất đã xác nhận, null nếu không có. */
    HotelBookingDTO findNextUpcoming(UUID userId);
}
