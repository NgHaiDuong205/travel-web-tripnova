package com.duong.travelweb.service;

import java.util.List;
import java.math.BigDecimal;
import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.CarAvailabilityDTO;
import com.duong.travelweb.model.dto.CarBookingDTO;
import com.duong.travelweb.model.dto.CarBookingRequestDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thuê xe. Mỗi xe là một chiếc cụ thể: hai booking không được giao nhau về thời gian.
 * Giá = price_per_day × số ngày (làm tròn lên theo 24h, tối thiểu 1). Giữ xe bằng booking pending trong hold-minutes;
 * huỷ miễn phí (hoàn 100%) tới 48h trước giờ nhận xe. car_bookings không có cột hoàn tiền nên chỉ hoàn toàn bộ
 * (status refunded).
 */
public interface CarBookingService extends OrderBookingHandler {
    int FREE_CANCELLATION_HOURS = 48;
    int MAX_RENTAL_DAYS = 30;

    CarAvailabilityDTO availability(UUID carId, LocalDateTime from, LocalDateTime to);

    CarBookingDTO create(UUID userId, CarBookingRequestDTO request);

    /** Kiểm tra như khi đặt (không giữ chỗ) và trả tổng tiền hiện tại; không hợp lệ -> ApiException. */
    BigDecimal quote(UUID userId, CarBookingRequestDTO request);

    /** Nhiều dòng (từ giỏ hàng) trong 1 order + 1 payment; lỗi ở dòng nào thì rollback toàn bộ. */
    CartCheckoutDTO createOrder(UUID userId, List<CarBookingRequestDTO> requests, String paymentMethod);

    /** Chủ booking hoặc admin, ngược lại 404. */
    CarBookingDTO get(UUID userId, boolean isAdmin, UUID bookingId);

    CarBookingDTO cancel(UUID userId, UUID bookingId, String reason);

    /** @param statusGroup all | upcoming | pending | completed | cancelled */
    Page<CarBookingDTO> findMine(UUID userId, String statusGroup, int page, int limit);

    // ---- Admin ----
    Page<CarBookingDTO> findForAdmin(String status, String keyword, int page, int limit);

    /**
     * pending→cancelled, confirmed→cancelled (hoàn 100%), confirmed→checked_in (đã giao xe),
     * checked_in→checked_out (đã trả xe), checked_out→completed, confirmed→no_show.
     */
    CarBookingDTO updateStatusByAdmin(UUID bookingId, String status, String reason);
}
