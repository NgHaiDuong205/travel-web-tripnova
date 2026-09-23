package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.CancelBookingRequestDTO;
import com.duong.travelweb.model.dto.HotelBookingCreatedDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.HotelBookingRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityCheckDTO;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class HotelBookingAPI {
    private final HotelBookingService hotelBookingService;

    public HotelBookingAPI(HotelBookingService hotelBookingService) {
        this.hotelBookingService = hotelBookingService;
    }

    @GetMapping("/api/hotel-bookings/check-availability/")
    public ResponseEntity<RoomAvailabilityCheckDTO> checkAvailability(
            @RequestParam("hotelId") UUID hotelId,
            @RequestParam("roomTypeId") UUID roomTypeId,
            @RequestParam(value = "roomId", required = false) UUID roomId,
            @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(hotelBookingService.checkAvailability(hotelId, roomTypeId, roomId, checkIn, checkOut));
    }

    @PostMapping("/api/hotel-bookings/")
    public ResponseEntity<HotelBookingCreatedDTO> createBooking(@Valid @RequestBody HotelBookingRequestDTO request) {
        HotelBookingCreatedDTO result = hotelBookingService.createBooking(SecurityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/api/hotel-bookings/{bookingId}/")
    public ResponseEntity<HotelBookingDTO> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(hotelBookingService.getBooking(SecurityUtil.getCurrentUserId(), bookingId));
    }

    @PostMapping("/api/hotel-bookings/{bookingId}/cancel/")
    public ResponseEntity<HotelBookingDTO> cancelBooking(@PathVariable("bookingId") UUID bookingId,
                                                        @Valid @RequestBody(required = false) CancelBookingRequestDTO request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(hotelBookingService.cancelBooking(SecurityUtil.getCurrentUserId(), bookingId, reason));
    }
}
