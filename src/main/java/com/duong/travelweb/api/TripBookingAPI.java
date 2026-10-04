package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.TripBookingDTO;
import com.duong.travelweb.model.dto.TripBookingPayRequestDTO;
import com.duong.travelweb.model.dto.TripBookingQuoteDTO;
import com.duong.travelweb.model.dto.TripBookingRequestDTO;
import com.duong.travelweb.service.TripBookingService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Đặt trọn gói (khách sạn + xe) từ lịch trình của tôi; /api/me/** yêu cầu đăng nhập. */
@RestController
public class TripBookingAPI {
    private final TripBookingService tripBookingService;

    public TripBookingAPI(TripBookingService tripBookingService) {
        this.tripBookingService = tripBookingService;
    }

    /** Báo giá: hạng phòng còn trống + số phòng cần, xe tại điểm đến, chính sách huỷ, chi phí trả tại chỗ. */
    @GetMapping("/api/me/itineraries/{itineraryId}/booking/quote/")
    public ResponseEntity<TripBookingQuoteDTO> quote(@PathVariable("itineraryId") UUID itineraryId,
                                                     @RequestParam(value = "hotelId", required = false) UUID hotelId) {
        return ResponseEntity.ok(tripBookingService.quote(SecurityUtil.getCurrentUserId(), itineraryId, hotelId));
    }

    @GetMapping("/api/me/itineraries/{itineraryId}/booking/")
    public ResponseEntity<TripBookingDTO> get(@PathVariable("itineraryId") UUID itineraryId) {
        return ResponseEntity.ok(tripBookingService.get(SecurityUtil.getCurrentUserId(), itineraryId));
    }

    /** Tạo các order chờ thanh toán (giữ chỗ). 409 nếu lịch trình đang có lượt đặt còn hiệu lực. */
    @PostMapping("/api/me/itineraries/{itineraryId}/booking/")
    public ResponseEntity<TripBookingDTO> book(@PathVariable("itineraryId") UUID itineraryId,
                                               @Valid @RequestBody TripBookingRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripBookingService.book(SecurityUtil.getCurrentUserId(), itineraryId, request));
    }

    /** Thanh toán (cổng giả lập, app.payment.mock-enabled) mọi order đang chờ của lượt đặt. */
    @PostMapping("/api/me/itineraries/{itineraryId}/booking/pay/")
    public ResponseEntity<TripBookingDTO> pay(@PathVariable("itineraryId") UUID itineraryId,
                                              @RequestBody(required = false) TripBookingPayRequestDTO request) {
        boolean success = request == null || request.isSuccess();
        return ResponseEntity.ok(tripBookingService.pay(SecurityUtil.getCurrentUserId(), itineraryId, success));
    }

    @PostMapping("/api/me/itineraries/{itineraryId}/booking/cancel/")
    public ResponseEntity<TripBookingDTO> cancel(@PathVariable("itineraryId") UUID itineraryId) {
        return ResponseEntity.ok(tripBookingService.cancel(SecurityUtil.getCurrentUserId(), itineraryId));
    }
}
