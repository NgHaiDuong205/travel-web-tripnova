package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.FlightBookingDTO;
import com.duong.travelweb.model.dto.FlightBookingRequestDTO;
import com.duong.travelweb.model.dto.FlightOrderCreatedDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Đặt vé máy bay: một order gồm nhiều vé (mỗi hành khách một ghế), chung một payment.
 * Ghế: available → held (khi đặt, khoá dòng ghế) → booked (khi thanh toán); huỷ / hết hạn → available.
 * Huỷ miễn phí tới 24h trước giờ bay, hoàn 100% từng vé (flight_bookings không có cột hoàn tiền → status refunded).
 */
public interface FlightBookingService extends OrderBookingHandler {
    int FREE_CANCELLATION_HOURS = 24;

    FlightOrderCreatedDTO create(UUID userId, FlightBookingRequestDTO request);

    FlightBookingDTO get(UUID userId, boolean isAdmin, UUID bookingId);

    /** Vé pending: huỷ cả order (chung một payment). Vé confirmed: hoàn riêng vé đó. */
    FlightBookingDTO cancel(UUID userId, UUID bookingId, String reason);

    /** Đổi ghế: khi còn pending thì tính lại tiền order; đã thanh toán thì chỉ đổi sang ghế cùng hạng, cùng giá. */
    FlightBookingDTO selectSeat(UUID userId, UUID bookingId, UUID seatId);

    /** @param statusGroup all | upcoming | pending | completed | cancelled */
    Page<FlightBookingDTO> findMine(UUID userId, String statusGroup, int page, int limit);

    // ---- Admin ----
    Page<FlightBookingDTO> findForAdmin(String status, String keyword, int page, int limit);

    /** pending→cancelled, confirmed→cancelled (hoàn 100%) | checked_in | no_show, checked_in→completed. */
    FlightBookingDTO updateStatusByAdmin(UUID bookingId, String status, String reason);
}
