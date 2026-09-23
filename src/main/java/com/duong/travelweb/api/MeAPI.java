package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.CancelBookingRequestDTO;
import com.duong.travelweb.model.dto.ChangePasswordRequestDTO;
import com.duong.travelweb.model.dto.DashboardDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.PaymentDTO;
import com.duong.travelweb.model.dto.PaymentSummaryDTO;
import com.duong.travelweb.model.dto.ProfileUpdateRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.service.AuthService;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.service.PaymentService;
import com.duong.travelweb.service.UserService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API cho user đang đăng nhập (tài khoản, lịch sử đặt phòng, thanh toán). */
@RestController
public class MeAPI {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final UserService userService;
    private final AuthService authService;
    private final HotelBookingService hotelBookingService;
    private final PaymentService paymentService;

    public MeAPI(UserService userService,
                 AuthService authService,
                 HotelBookingService hotelBookingService,
                 PaymentService paymentService) {
        this.userService = userService;
        this.authService = authService;
        this.hotelBookingService = hotelBookingService;
        this.paymentService = paymentService;
    }

    @GetMapping("/api/me/profile/")
    public ResponseEntity<UserDTO> getProfile() {
        return ResponseEntity.ok(userService.getProfile(SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/api/me/profile/")
    public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody ProfileUpdateRequestDTO request) {
        return ResponseEntity.ok(userService.updateProfile(SecurityUtil.getCurrentUserId(), request));
    }

    @PutMapping("/api/me/change-password/")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(SecurityUtil.getCurrentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/me/dashboard/")
    public ResponseEntity<DashboardDTO> getDashboard() {
        return ResponseEntity.ok(userService.getDashboard(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/api/me/bookings/")
    public ResponseEntity<List<HotelBookingDTO>> getMyBookings(@RequestParam(value = "status", required = false) String status,
                                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                                               @RequestParam(value = "limit", defaultValue = "10") int limit) {
        UUID userId = SecurityUtil.getCurrentUserId();
        int size = clampLimit(limit);
        List<HotelBookingDTO> results = hotelBookingService.findMyBookings(userId, status, page, size);
        long total = hotelBookingService.countMyBookings(userId, status);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .body(results);
    }

    @GetMapping("/api/me/bookings/{bookingId}/")
    public ResponseEntity<HotelBookingDTO> getMyBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(hotelBookingService.getBooking(SecurityUtil.getCurrentUserId(), bookingId));
    }

    @PostMapping("/api/me/bookings/{bookingId}/cancel/")
    public ResponseEntity<HotelBookingDTO> cancelMyBooking(@PathVariable("bookingId") UUID bookingId,
                                                          @Valid @RequestBody(required = false) CancelBookingRequestDTO request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(hotelBookingService.cancelBooking(SecurityUtil.getCurrentUserId(), bookingId, reason));
    }

    @GetMapping("/api/me/payments/")
    public ResponseEntity<List<PaymentDTO>> getMyPayments(@RequestParam(value = "status", required = false) String status,
                                                          @RequestParam(value = "page", defaultValue = "1") int page,
                                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        Page<PaymentDTO> results = paymentService.findMyPayments(SecurityUtil.getCurrentUserId(), status, page, clampLimit(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(results.getTotalElements()))
                .body(results.getContent());
    }

    @GetMapping("/api/me/payments/summary/")
    public ResponseEntity<PaymentSummaryDTO> getMyPaymentSummary() {
        return ResponseEntity.ok(paymentService.getMySummary(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/api/me/payments/{paymentId}/")
    public ResponseEntity<PaymentDTO> getMyPayment(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(SecurityUtil.getCurrentUserId(), paymentId));
    }

    private int clampLimit(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
