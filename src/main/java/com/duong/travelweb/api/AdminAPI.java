package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AdminDashboardDTO;
import com.duong.travelweb.model.dto.ContactMessageDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.PaymentDTO;
import com.duong.travelweb.model.dto.StatusUpdateRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.dto.UserRolesRequestDTO;
import com.duong.travelweb.model.dto.UserStatusRequestDTO;
import com.duong.travelweb.service.AdminDashboardService;
import com.duong.travelweb.service.ContactService;
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

/** API quản trị (dashboard, booking, user, payment, liên hệ). /api/admin/** yêu cầu ROLE_ADMIN (SecurityConfig). */
@RestController
public class AdminAPI {
    private static final int MAX_LIMIT = 100;

    private final AdminDashboardService adminDashboardService;
    private final HotelBookingService hotelBookingService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final ContactService contactService;

    public AdminAPI(AdminDashboardService adminDashboardService,
                    HotelBookingService hotelBookingService,
                    UserService userService,
                    PaymentService paymentService,
                    ContactService contactService) {
        this.adminDashboardService = adminDashboardService;
        this.hotelBookingService = hotelBookingService;
        this.userService = userService;
        this.paymentService = paymentService;
        this.contactService = contactService;
    }

    @GetMapping("/api/admin/dashboard/")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminDashboardService.getDashboard());
    }

    // ---- Hotel bookings ----

    @GetMapping("/api/admin/hotel-bookings/")
    public ResponseEntity<List<HotelBookingDTO>> getBookings(@RequestParam(value = "status", required = false) String status,
                                                             @RequestParam(value = "q", required = false) String keyword,
                                                             @RequestParam(value = "page", defaultValue = "1") int page,
                                                             @RequestParam(value = "limit", defaultValue = "20") int limit) {
        List<HotelBookingDTO> results = hotelBookingService.findForAdmin(status, keyword, page, clamp(limit));
        long total = hotelBookingService.countForAdmin(status, keyword);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(total)).body(results);
    }

    @GetMapping("/api/admin/hotel-bookings/{bookingId}/")
    public ResponseEntity<HotelBookingDTO> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(hotelBookingService.getBooking(SecurityUtil.getCurrentUserId(), bookingId));
    }

    @PutMapping("/api/admin/hotel-bookings/{bookingId}/status/")
    public ResponseEntity<HotelBookingDTO> updateBookingStatus(@PathVariable("bookingId") UUID bookingId,
                                                              @Valid @RequestBody StatusUpdateRequestDTO request) {
        return ResponseEntity.ok(hotelBookingService.updateStatusByAdmin(bookingId, request.getStatus(), request.getReason()));
    }

    // ---- Users ----

    @GetMapping("/api/admin/users/")
    public ResponseEntity<List<UserDTO>> getUsers(@RequestParam(value = "q", required = false) String keyword,
                                                  @RequestParam(value = "role", required = false) String role,
                                                  @RequestParam(value = "status", required = false) String status,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "limit", defaultValue = "20") int limit) {
        String roleFilter = role == null || role.isBlank() || "all".equals(role) ? null : role;
        Boolean active = switch (status == null ? "" : status) {
            case "active" -> true;
            case "locked" -> false;
            default -> null;
        };
        List<UserDTO> results = userService.findForAdmin(keyword, roleFilter, active, page, clamp(limit));
        long total = userService.countForAdmin(keyword, roleFilter, active);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(total)).body(results);
    }

    @GetMapping("/api/admin/users/{userId}/")
    public ResponseEntity<UserDTO> getUser(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(userService.getUserForAdmin(userId));
    }

    @PutMapping("/api/admin/users/{userId}/status/")
    public ResponseEntity<UserDTO> updateUserStatus(@PathVariable("userId") UUID userId,
                                                    @Valid @RequestBody UserStatusRequestDTO request) {
        return ResponseEntity.ok(userService.updateStatus(SecurityUtil.getCurrentUserId(), userId, request.getIsActive()));
    }

    @PutMapping("/api/admin/users/{userId}/roles/")
    public ResponseEntity<UserDTO> updateUserRoles(@PathVariable("userId") UUID userId,
                                                   @Valid @RequestBody UserRolesRequestDTO request) {
        return ResponseEntity.ok(userService.updateRoles(SecurityUtil.getCurrentUserId(), userId, request.getRoles()));
    }

    // ---- Payments ----

    @GetMapping("/api/admin/payments/")
    public ResponseEntity<List<PaymentDTO>> getPayments(@RequestParam(value = "status", required = false) String status,
                                                        @RequestParam(value = "page", defaultValue = "1") int page,
                                                        @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<PaymentDTO> results = paymentService.findAllForAdmin(status, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(results.getTotalElements()))
                .body(results.getContent());
    }

    // ---- Contact messages ----

    @GetMapping("/api/admin/contact-messages/")
    public ResponseEntity<List<ContactMessageDTO>> getContactMessages(@RequestParam(value = "status", required = false) String status,
                                                                      @RequestParam(value = "page", defaultValue = "1") int page,
                                                                      @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<ContactMessageDTO> results = contactService.findMessages(status, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(results.getTotalElements()))
                .body(results.getContent());
    }

    @PutMapping("/api/admin/contact-messages/{messageId}/status/")
    public ResponseEntity<ContactMessageDTO> updateContactStatus(@PathVariable("messageId") UUID messageId,
                                                                 @Valid @RequestBody StatusUpdateRequestDTO request) {
        return ResponseEntity.ok(contactService.updateStatus(messageId, request.getStatus()));
    }

    private int clamp(int limit) {
        return limit < 1 ? 20 : Math.min(limit, MAX_LIMIT);
    }
}
