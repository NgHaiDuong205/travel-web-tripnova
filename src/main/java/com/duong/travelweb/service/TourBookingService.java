package com.duong.travelweb.service;

import java.util.List;
import java.math.BigDecimal;
import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.TourBookingDTO;
import com.duong.travelweb.model.dto.TourBookingRequestDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Đặt tour. Tour có departure_date thì chỉ đặt đúng ngày đó, không có thì khách chọn ngày (tối thiểu MIN_LEAD_DAYS ngày tới).
 * max_participants là số chỗ mỗi ngày khởi hành (khoá dòng tours khi đặt / xác nhận).
 * Giá = người lớn × price_adult + trẻ em × price_child (không có thì bằng giá người lớn).
 * Huỷ miễn phí tới 30 ngày trước khởi hành, hoàn 100% (tour_bookings không có cột hoàn tiền → status refunded).
 */
public interface TourBookingService extends OrderBookingHandler {
    int FREE_CANCELLATION_DAYS = 30;
    int MIN_LEAD_DAYS = 3;

    TourBookingDTO create(UUID userId, TourBookingRequestDTO request);

    /** Kiểm tra như khi đặt (không giữ chỗ) và trả tổng tiền hiện tại; không hợp lệ -> ApiException. */
    BigDecimal quote(UUID userId, TourBookingRequestDTO request);

    /** Nhiều dòng (từ giỏ hàng) trong 1 order + 1 payment; lỗi ở dòng nào thì rollback toàn bộ. */
    CartCheckoutDTO createOrder(UUID userId, List<TourBookingRequestDTO> requests, String paymentMethod);

    TourBookingDTO get(UUID userId, boolean isAdmin, UUID bookingId);

    TourBookingDTO cancel(UUID userId, UUID bookingId, String reason);

    /** @param statusGroup all | upcoming | pending | completed | cancelled */
    Page<TourBookingDTO> findMine(UUID userId, String statusGroup, int page, int limit);

    // ---- Admin ----
    Page<TourBookingDTO> findForAdmin(String status, String keyword, int page, int limit);

    /** pending→cancelled, confirmed→cancelled (hoàn 100%) | checked_in | no_show, checked_in→completed. */
    TourBookingDTO updateStatusByAdmin(UUID bookingId, String status, String reason);
}
