package com.duong.travelweb.api;

import com.duong.travelweb.builder.CarSearchBuilder;
import com.duong.travelweb.model.dto.CancelBookingRequestDTO;
import com.duong.travelweb.model.dto.CarAvailabilityDTO;
import com.duong.travelweb.model.dto.CarBookingDTO;
import com.duong.travelweb.model.dto.CarBookingRequestDTO;
import com.duong.travelweb.model.dto.CarDTO;
import com.duong.travelweb.model.dto.CarFilterOptionsDTO;
import com.duong.travelweb.service.CarBookingService;
import com.duong.travelweb.service.CarService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Xe cho thuê (GET /api/cars/** công khai) và đặt xe (/api/car-bookings/**, /api/me/car-bookings/ — cần đăng nhập). */
@RestController
public class CarAPI {
    private static final int MAX_LIMIT = 100;

    private final CarService carService;
    private final CarBookingService carBookingService;

    public CarAPI(CarService carService, CarBookingService carBookingService) {
        this.carService = carService;
        this.carBookingService = carBookingService;
    }

    /**
     * ?q&destinationId&type&brands=a,b&seats (tối thiểu)&transmission&fuelType&withDriver&priceMin&priceMax
     * &pickupDate&returnDate (ISO date-time, chỉ lấy xe còn trống)&sort=newest|price_asc|price_desc|seats_desc|name&page&limit
     */
    @GetMapping("/api/cars/")
    public ResponseEntity<List<CarDTO>> getCars(@RequestParam(value = "q", required = false) String q,
                                                @RequestParam(value = "destinationId", required = false) UUID destinationId,
                                                @RequestParam(value = "type", required = false) String type,
                                                @RequestParam(value = "brands", required = false) String brands,
                                                @RequestParam(value = "seats", required = false) Integer seats,
                                                @RequestParam(value = "transmission", required = false) String transmission,
                                                @RequestParam(value = "fuelType", required = false) String fuelType,
                                                @RequestParam(value = "withDriver", required = false) Boolean withDriver,
                                                @RequestParam(value = "priceMin", required = false) BigDecimal priceMin,
                                                @RequestParam(value = "priceMax", required = false) BigDecimal priceMax,
                                                @RequestParam(value = "pickupDate", required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pickupDate,
                                                @RequestParam(value = "returnDate", required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime returnDate,
                                                @RequestParam(value = "sort", required = false) String sort,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(value = "limit", defaultValue = "12") int limit) {
        CarSearchBuilder criteria = new CarSearchBuilder.Builder()
                .active(true)
                .keyword(blankToNull(q))
                .destinationId(destinationId)
                .carType(blankToNull(type))
                .brands(splitList(brands))
                .minSeats(seats)
                .transmission(blankToNull(transmission))
                .fuelType(blankToNull(fuelType))
                .withDriver(withDriver)
                .priceMin(priceMin)
                .priceMax(priceMax)
                .availableFrom(pickupDate)
                .availableTo(returnDate)
                .sort(sort)
                .build();
        return toResponse(carService.search(criteria, page, clamp(limit)));
    }

    /** Giá trị cho bộ lọc: loại xe, hãng, hộp số, nhiên liệu, khoảng giá. */
    @GetMapping("/api/cars/filters/")
    public ResponseEntity<CarFilterOptionsDTO> getFilters() {
        return ResponseEntity.ok(carService.filterOptions());
    }

    @GetMapping("/api/cars/{carId}/")
    public ResponseEntity<CarDTO> getCar(@PathVariable("carId") UUID carId) {
        return ResponseEntity.ok(carService.getPublic(carId));
    }

    /** ?from&to (ISO date-time): còn trống không, giá tạm tính và các khoảng đã có người thuê. */
    @GetMapping("/api/cars/{carId}/availability/")
    public ResponseEntity<CarAvailabilityDTO> getAvailability(@PathVariable("carId") UUID carId,
                                                              @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                              @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(carBookingService.availability(carId, from, to));
    }

    /** Tạo đơn thuê (pending, giữ xe hold-minutes) + payment; FE chuyển tới /payment/{paymentId}. */
    @PostMapping("/api/car-bookings/")
    public ResponseEntity<CarBookingDTO> createBooking(@Valid @RequestBody CarBookingRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carBookingService.create(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/api/car-bookings/{bookingId}/")
    public ResponseEntity<CarBookingDTO> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(carBookingService.get(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), bookingId));
    }

    @PostMapping("/api/car-bookings/{bookingId}/cancel/")
    public ResponseEntity<CarBookingDTO> cancelBooking(@PathVariable("bookingId") UUID bookingId,
                                                       @RequestBody(required = false) CancelBookingRequestDTO body) {
        String reason = body == null ? null : body.getReason();
        return ResponseEntity.ok(carBookingService.cancel(SecurityUtil.getCurrentUserId(), bookingId, reason));
    }

    /** ?status=all|upcoming|pending|completed|cancelled */
    @GetMapping("/api/me/car-bookings/")
    public ResponseEntity<List<CarBookingDTO>> getMyBookings(@RequestParam(value = "status", required = false) String status,
                                                             @RequestParam(value = "page", defaultValue = "1") int page,
                                                             @RequestParam(value = "limit", defaultValue = "10") int limit) {
        Page<CarBookingDTO> result = carBookingService.findMine(SecurityUtil.getCurrentUserId(), status, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    private ResponseEntity<List<CarDTO>> toResponse(Page<CarDTO> result) {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    private static List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int clamp(int limit) {
        return limit < 1 ? 12 : Math.min(limit, MAX_LIMIT);
    }
}
