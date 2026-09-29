package com.duong.travelweb.api;

import com.duong.travelweb.builder.CarSearchBuilder;
import com.duong.travelweb.model.dto.CarBookingDTO;
import com.duong.travelweb.model.dto.CarDTO;
import com.duong.travelweb.model.dto.CarRequestDTO;
import com.duong.travelweb.model.dto.StatusUpdateRequestDTO;
import com.duong.travelweb.service.CarBookingService;
import com.duong.travelweb.service.CarService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Quản trị xe và đơn thuê xe. /api/admin/** yêu cầu ROLE_ADMIN (SecurityConfig). */
@RestController
public class AdminCarAPI {
    private static final int MAX_LIMIT = 100;

    private final CarService carService;
    private final CarBookingService carBookingService;

    public AdminCarAPI(CarService carService, CarBookingService carBookingService) {
        this.carService = carService;
        this.carBookingService = carBookingService;
    }

    /** ?q&type&active=true|false&destinationId&sort&page&limit */
    @GetMapping("/api/admin/cars/")
    public ResponseEntity<List<CarDTO>> getCars(@RequestParam(value = "q", required = false) String q,
                                                @RequestParam(value = "type", required = false) String type,
                                                @RequestParam(value = "active", required = false) Boolean active,
                                                @RequestParam(value = "destinationId", required = false) UUID destinationId,
                                                @RequestParam(value = "sort", required = false) String sort,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(value = "limit", defaultValue = "20") int limit) {
        CarSearchBuilder criteria = new CarSearchBuilder.Builder()
                .keyword(blankToNull(q))
                .carType(blankToNull(type))
                .active(active)
                .destinationId(destinationId)
                .sort(sort)
                .build();
        Page<CarDTO> result = carService.search(criteria, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @PostMapping("/api/admin/cars/")
    public ResponseEntity<CarDTO> createCar(@Valid @RequestBody CarRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carService.create(request));
    }

    @GetMapping("/api/admin/cars/{carId}/")
    public ResponseEntity<CarDTO> getCar(@PathVariable("carId") UUID carId) {
        return ResponseEntity.ok(carService.get(carId));
    }

    @PutMapping("/api/admin/cars/{carId}/")
    public ResponseEntity<CarDTO> updateCar(@PathVariable("carId") UUID carId, @Valid @RequestBody CarRequestDTO request) {
        return ResponseEntity.ok(carService.update(carId, request));
    }

    /** Xoá mềm: ngừng cho thuê, đơn đã đặt vẫn giữ. */
    @DeleteMapping("/api/admin/cars/{carId}/")
    public ResponseEntity<Void> deleteCar(@PathVariable("carId") UUID carId) {
        carService.deactivate(carId);
        return ResponseEntity.noContent().build();
    }

    /** ?status&q (mã order, email/tên khách, tên xe, biển số)&page&limit */
    @GetMapping("/api/admin/car-bookings/")
    public ResponseEntity<List<CarBookingDTO>> getBookings(@RequestParam(value = "status", required = false) String status,
                                                           @RequestParam(value = "q", required = false) String q,
                                                           @RequestParam(value = "page", defaultValue = "1") int page,
                                                           @RequestParam(value = "limit", defaultValue = "20") int limit) {
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : status.trim();
        Page<CarBookingDTO> result = carBookingService.findForAdmin(filter, blankToNull(q), page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @GetMapping("/api/admin/car-bookings/{bookingId}/")
    public ResponseEntity<CarBookingDTO> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(carBookingService.get(SecurityUtil.getCurrentUserId(), true, bookingId));
    }

    @PutMapping("/api/admin/car-bookings/{bookingId}/status/")
    public ResponseEntity<CarBookingDTO> updateBookingStatus(@PathVariable("bookingId") UUID bookingId,
                                                             @Valid @RequestBody StatusUpdateRequestDTO request) {
        return ResponseEntity.ok(carBookingService.updateStatusByAdmin(bookingId, request.getStatus(), request.getReason()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int clamp(int limit) {
        return limit < 1 ? 20 : Math.min(limit, MAX_LIMIT);
    }
}
