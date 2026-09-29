package com.duong.travelweb.api;

import com.duong.travelweb.builder.TourSearchBuilder;
import com.duong.travelweb.model.dto.StatusUpdateRequestDTO;
import com.duong.travelweb.model.dto.TourBookingDTO;
import com.duong.travelweb.model.dto.TourCarDTO;
import com.duong.travelweb.model.dto.TourDTO;
import com.duong.travelweb.model.dto.TourFlightDTO;
import com.duong.travelweb.model.dto.TourHotelDTO;
import com.duong.travelweb.model.dto.TourLinkRequestDTO;
import com.duong.travelweb.model.dto.TourRequestDTO;
import com.duong.travelweb.service.TourBookingService;
import com.duong.travelweb.service.TourService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Quản trị tour, khách sạn / xe / chuyến bay kèm tour và đơn tour. /api/admin/** yêu cầu ROLE_ADMIN. */
@RestController
public class AdminTourAPI {
    private static final int MAX_LIMIT = 100;

    private final TourService tourService;
    private final TourBookingService tourBookingService;

    public AdminTourAPI(TourService tourService, TourBookingService tourBookingService) {
        this.tourService = tourService;
        this.tourBookingService = tourBookingService;
    }

    /** ?q&active&destinationId&sort&page&limit */
    @GetMapping("/api/admin/tours/")
    public ResponseEntity<List<TourDTO>> getTours(@RequestParam(value = "q", required = false) String q,
                                                  @RequestParam(value = "active", required = false) Boolean active,
                                                  @RequestParam(value = "destinationId", required = false) UUID destinationId,
                                                  @RequestParam(value = "sort", required = false) String sort,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "limit", defaultValue = "20") int limit) {
        TourSearchBuilder criteria = new TourSearchBuilder.Builder()
                .keyword(q == null || q.isBlank() ? null : q.trim())
                .active(active)
                .destinationId(destinationId)
                .sort(sort)
                .build();
        Page<TourDTO> result = tourService.search(criteria, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @PostMapping("/api/admin/tours/")
    public ResponseEntity<TourDTO> createTour(@Valid @RequestBody TourRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.create(request));
    }

    @GetMapping("/api/admin/tours/{tourId}/")
    public ResponseEntity<TourDTO> getTour(@PathVariable("tourId") UUID tourId) {
        return ResponseEntity.ok(tourService.get(tourId));
    }

    @PutMapping("/api/admin/tours/{tourId}/")
    public ResponseEntity<TourDTO> updateTour(@PathVariable("tourId") UUID tourId, @Valid @RequestBody TourRequestDTO request) {
        return ResponseEntity.ok(tourService.update(tourId, request));
    }

    /** Xoá mềm: ngừng bán, đơn đã đặt vẫn giữ. */
    @DeleteMapping("/api/admin/tours/{tourId}/")
    public ResponseEntity<Void> deleteTour(@PathVariable("tourId") UUID tourId) {
        tourService.deactivate(tourId);
        return ResponseEntity.noContent().build();
    }

    /** Gắn / cập nhật khách sạn {checkInDay?, nights?}; trả danh sách khách sạn của tour. */
    @PostMapping("/api/admin/tours/{tourId}/hotels/{hotelId}/")
    public ResponseEntity<List<TourHotelDTO>> linkHotel(@PathVariable("tourId") UUID tourId, @PathVariable("hotelId") UUID hotelId,
                                                        @Valid @RequestBody(required = false) TourLinkRequestDTO request) {
        return ResponseEntity.ok(tourService.linkHotel(tourId, hotelId, request));
    }

    @DeleteMapping("/api/admin/tours/{tourId}/hotels/{hotelId}/")
    public ResponseEntity<Void> unlinkHotel(@PathVariable("tourId") UUID tourId, @PathVariable("hotelId") UUID hotelId) {
        tourService.unlinkHotel(tourId, hotelId);
        return ResponseEntity.noContent().build();
    }

    /** Gắn / cập nhật xe {usageDay?}. */
    @PostMapping("/api/admin/tours/{tourId}/cars/{carId}/")
    public ResponseEntity<List<TourCarDTO>> linkCar(@PathVariable("tourId") UUID tourId, @PathVariable("carId") UUID carId,
                                                    @Valid @RequestBody(required = false) TourLinkRequestDTO request) {
        return ResponseEntity.ok(tourService.linkCar(tourId, carId, request));
    }

    @DeleteMapping("/api/admin/tours/{tourId}/cars/{carId}/")
    public ResponseEntity<Void> unlinkCar(@PathVariable("tourId") UUID tourId, @PathVariable("carId") UUID carId) {
        tourService.unlinkCar(tourId, carId);
        return ResponseEntity.noContent().build();
    }

    /** Gắn / cập nhật chuyến bay {leg?: outbound | return | ...}. */
    @PostMapping("/api/admin/tours/{tourId}/flights/{flightId}/")
    public ResponseEntity<List<TourFlightDTO>> linkFlight(@PathVariable("tourId") UUID tourId, @PathVariable("flightId") UUID flightId,
                                                          @Valid @RequestBody(required = false) TourLinkRequestDTO request) {
        return ResponseEntity.ok(tourService.linkFlight(tourId, flightId, request));
    }

    @DeleteMapping("/api/admin/tours/{tourId}/flights/{flightId}/")
    public ResponseEntity<Void> unlinkFlight(@PathVariable("tourId") UUID tourId, @PathVariable("flightId") UUID flightId) {
        tourService.unlinkFlight(tourId, flightId);
        return ResponseEntity.noContent().build();
    }

    /** ?status&q (mã order, khách, người liên hệ, tên tour)&page&limit */
    @GetMapping("/api/admin/tour-bookings/")
    public ResponseEntity<List<TourBookingDTO>> getBookings(@RequestParam(value = "status", required = false) String status,
                                                            @RequestParam(value = "q", required = false) String q,
                                                            @RequestParam(value = "page", defaultValue = "1") int page,
                                                            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : status.trim();
        Page<TourBookingDTO> result = tourBookingService.findForAdmin(filter, q == null || q.isBlank() ? null : q.trim(), page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @GetMapping("/api/admin/tour-bookings/{bookingId}/")
    public ResponseEntity<TourBookingDTO> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(tourBookingService.get(null, true, bookingId));
    }

    @PutMapping("/api/admin/tour-bookings/{bookingId}/status/")
    public ResponseEntity<TourBookingDTO> updateBookingStatus(@PathVariable("bookingId") UUID bookingId,
                                                              @Valid @RequestBody StatusUpdateRequestDTO request) {
        return ResponseEntity.ok(tourBookingService.updateStatusByAdmin(bookingId, request.getStatus(), request.getReason()));
    }

    private int clamp(int limit) {
        return limit < 1 ? 20 : Math.min(limit, MAX_LIMIT);
    }
}
