package com.duong.travelweb.api;

import com.duong.travelweb.builder.FlightSearchBuilder;
import com.duong.travelweb.model.dto.FlightBookingDTO;
import com.duong.travelweb.model.dto.FlightDTO;
import com.duong.travelweb.model.dto.FlightRequestDTO;
import com.duong.travelweb.model.dto.FlightSeatDTO;
import com.duong.travelweb.model.dto.FlightSeatRequestDTO;
import com.duong.travelweb.model.dto.SeatMapGenerateRequestDTO;
import com.duong.travelweb.model.dto.StatusUpdateRequestDTO;
import com.duong.travelweb.service.FlightBookingService;
import com.duong.travelweb.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Quản trị chuyến bay, ghế và vé. /api/admin/** yêu cầu ROLE_ADMIN (SecurityConfig). */
@RestController
public class AdminFlightAPI {
    private static final int MAX_LIMIT = 100;

    private final FlightService flightService;
    private final FlightBookingService flightBookingService;

    public AdminFlightAPI(FlightService flightService, FlightBookingService flightBookingService) {
        this.flightService = flightService;
        this.flightBookingService = flightBookingService;
    }

    /** ?q (số hiệu, hãng, thành phố, mã sân bay)&sort&page&limit — gồm cả chuyến đã bay / ngừng bán. */
    @GetMapping("/api/admin/flights/")
    public ResponseEntity<List<FlightDTO>> getFlights(@RequestParam(value = "q", required = false) String q,
                                                      @RequestParam(value = "sort", required = false) String sort,
                                                      @RequestParam(value = "page", defaultValue = "1") int page,
                                                      @RequestParam(value = "limit", defaultValue = "20") int limit) {
        FlightSearchBuilder criteria = new FlightSearchBuilder.Builder()
                .keyword(q == null || q.isBlank() ? null : q.trim())
                .sort(sort == null || sort.isBlank() ? "newest" : sort)
                .build();
        Page<FlightDTO> result = flightService.search(criteria, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @PostMapping("/api/admin/flights/")
    public ResponseEntity<FlightDTO> createFlight(@Valid @RequestBody FlightRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.create(request));
    }

    @GetMapping("/api/admin/flights/{flightId}/")
    public ResponseEntity<FlightDTO> getFlight(@PathVariable("flightId") UUID flightId) {
        return ResponseEntity.ok(flightService.get(flightId));
    }

    @PutMapping("/api/admin/flights/{flightId}/")
    public ResponseEntity<FlightDTO> updateFlight(@PathVariable("flightId") UUID flightId, @Valid @RequestBody FlightRequestDTO request) {
        return ResponseEntity.ok(flightService.update(flightId, request));
    }

    /** Xoá mềm: ngừng bán, vé đã đặt vẫn giữ. */
    @DeleteMapping("/api/admin/flights/{flightId}/")
    public ResponseEntity<Void> deleteFlight(@PathVariable("flightId") UUID flightId) {
        flightService.deactivate(flightId);
        return ResponseEntity.noContent().build();
    }

    /** Sơ đồ ghế đầy đủ trạng thái (available | held | booked | blocked). */
    @GetMapping("/api/admin/flights/{flightId}/seats/")
    public ResponseEntity<List<FlightSeatDTO>> getSeats(@PathVariable("flightId") UUID flightId) {
        return ResponseEntity.ok(flightService.seats(flightId, null, true));
    }

    @PostMapping("/api/admin/flights/{flightId}/seats/")
    public ResponseEntity<FlightSeatDTO> addSeat(@PathVariable("flightId") UUID flightId, @Valid @RequestBody FlightSeatRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.addSeat(flightId, request));
    }

    /** Sinh lại sơ đồ theo khối; ghế đã từng có vé được giữ nguyên. */
    @PostMapping("/api/admin/flights/{flightId}/seats/generate/")
    public ResponseEntity<List<FlightSeatDTO>> generateSeats(@PathVariable("flightId") UUID flightId,
                                                             @Valid @RequestBody SeatMapGenerateRequestDTO request) {
        return ResponseEntity.ok(flightService.generateSeatMap(flightId, request));
    }

    @PutMapping("/api/admin/flights/{flightId}/seats/{seatId}/")
    public ResponseEntity<FlightSeatDTO> updateSeat(@PathVariable("flightId") UUID flightId, @PathVariable("seatId") UUID seatId,
                                                    @Valid @RequestBody FlightSeatRequestDTO request) {
        return ResponseEntity.ok(flightService.updateSeat(flightId, seatId, request));
    }

    @DeleteMapping("/api/admin/flights/{flightId}/seats/{seatId}/")
    public ResponseEntity<Void> deleteSeat(@PathVariable("flightId") UUID flightId, @PathVariable("seatId") UUID seatId) {
        flightService.deleteSeat(flightId, seatId);
        return ResponseEntity.noContent().build();
    }

    /** ?status&q (mã order, khách, hành khách, số hiệu)&page&limit */
    @GetMapping("/api/admin/flight-bookings/")
    public ResponseEntity<List<FlightBookingDTO>> getBookings(@RequestParam(value = "status", required = false) String status,
                                                              @RequestParam(value = "q", required = false) String q,
                                                              @RequestParam(value = "page", defaultValue = "1") int page,
                                                              @RequestParam(value = "limit", defaultValue = "20") int limit) {
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : status.trim();
        Page<FlightBookingDTO> result = flightBookingService.findForAdmin(filter, q == null || q.isBlank() ? null : q.trim(),
                page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @GetMapping("/api/admin/flight-bookings/{bookingId}/")
    public ResponseEntity<FlightBookingDTO> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(flightBookingService.get(null, true, bookingId));
    }

    @PutMapping("/api/admin/flight-bookings/{bookingId}/status/")
    public ResponseEntity<FlightBookingDTO> updateBookingStatus(@PathVariable("bookingId") UUID bookingId,
                                                                @Valid @RequestBody StatusUpdateRequestDTO request) {
        return ResponseEntity.ok(flightBookingService.updateStatusByAdmin(bookingId, request.getStatus(), request.getReason()));
    }

    private int clamp(int limit) {
        return limit < 1 ? 20 : Math.min(limit, MAX_LIMIT);
    }
}
