package com.duong.travelweb.api;

import com.duong.travelweb.builder.TourSearchBuilder;
import com.duong.travelweb.model.dto.CancelBookingRequestDTO;
import com.duong.travelweb.model.dto.TourAvailabilityDTO;
import com.duong.travelweb.model.dto.TourBookingDTO;
import com.duong.travelweb.model.dto.TourBookingRequestDTO;
import com.duong.travelweb.model.dto.TourCarDTO;
import com.duong.travelweb.model.dto.TourDTO;
import com.duong.travelweb.model.dto.TourDayDTO;
import com.duong.travelweb.model.dto.TourFlightDTO;
import com.duong.travelweb.model.dto.TourHotelDTO;
import com.duong.travelweb.service.TourBookingService;
import com.duong.travelweb.service.TourService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tour (GET /api/tours/** công khai) và đặt tour (/api/tour-bookings/**, /api/me/tour-bookings/ — cần đăng nhập). */
@RestController
public class TourAPI {
    private static final int MAX_LIMIT = 100;

    private final TourService tourService;
    private final TourBookingService tourBookingService;

    public TourAPI(TourService tourService, TourBookingService tourBookingService) {
        this.tourService = tourService;
        this.tourBookingService = tourBookingService;
    }

    /** ?q&destinationId&priceMin&priceMax&minDays&maxDays&sort=newest|price_asc|price_desc|duration_asc|duration_desc|departure&page&limit */
    @GetMapping("/api/tours/")
    public ResponseEntity<List<TourDTO>> getTours(@RequestParam(value = "q", required = false) String q,
                                                  @RequestParam(value = "destinationId", required = false) UUID destinationId,
                                                  @RequestParam(value = "priceMin", required = false) BigDecimal priceMin,
                                                  @RequestParam(value = "priceMax", required = false) BigDecimal priceMax,
                                                  @RequestParam(value = "minDays", required = false) Integer minDays,
                                                  @RequestParam(value = "maxDays", required = false) Integer maxDays,
                                                  @RequestParam(value = "sort", required = false) String sort,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "limit", defaultValue = "9") int limit) {
        TourSearchBuilder criteria = new TourSearchBuilder.Builder()
                .active(true)
                .keyword(q == null || q.isBlank() ? null : q.trim())
                .destinationId(destinationId)
                .priceMin(priceMin)
                .priceMax(priceMax)
                .minDays(minDays)
                .maxDays(maxDays)
                .sort(sort)
                .build();
        return toResponse(tourService.search(criteria, page, clamp(limit)));
    }

    /** Tour của một điểm đến (shortcut cho FE). */
    @GetMapping("/api/destinations/{destinationId}/tours/")
    public ResponseEntity<List<TourDTO>> getDestinationTours(@PathVariable("destinationId") UUID destinationId,
                                                             @RequestParam(value = "page", defaultValue = "1") int page,
                                                             @RequestParam(value = "limit", defaultValue = "9") int limit) {
        TourSearchBuilder criteria = new TourSearchBuilder.Builder().active(true).destinationId(destinationId).build();
        return toResponse(tourService.search(criteria, page, clamp(limit)));
    }

    @GetMapping("/api/tours/{tourId}/")
    public ResponseEntity<TourDTO> getTour(@PathVariable("tourId") UUID tourId) {
        return ResponseEntity.ok(tourService.getPublic(tourId));
    }

    @GetMapping("/api/tours/{tourId}/itinerary/")
    public ResponseEntity<List<TourDayDTO>> getItinerary(@PathVariable("tourId") UUID tourId) {
        return ResponseEntity.ok(tourService.itinerary(tourId));
    }

    /** ?date (yyyy-MM-dd; bỏ trống = ngày khởi hành cố định của tour): số chỗ còn lại. */
    @GetMapping("/api/tours/{tourId}/availability/")
    public ResponseEntity<TourAvailabilityDTO> getAvailability(@PathVariable("tourId") UUID tourId,
                                                               @RequestParam(value = "date", required = false)
                                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(tourService.availability(tourId, date));
    }

    @GetMapping("/api/tours/{tourId}/hotels/")
    public ResponseEntity<List<TourHotelDTO>> getHotels(@PathVariable("tourId") UUID tourId) {
        return ResponseEntity.ok(tourService.hotels(tourId));
    }

    @GetMapping("/api/tours/{tourId}/cars/")
    public ResponseEntity<List<TourCarDTO>> getCars(@PathVariable("tourId") UUID tourId) {
        return ResponseEntity.ok(tourService.cars(tourId));
    }

    @GetMapping("/api/tours/{tourId}/flights/")
    public ResponseEntity<List<TourFlightDTO>> getFlights(@PathVariable("tourId") UUID tourId) {
        return ResponseEntity.ok(tourService.flights(tourId));
    }

    /** Tạo đơn (pending, giữ chỗ hold-minutes) + payment; FE chuyển tới /trip-payment/{paymentId}. */
    @PostMapping("/api/tour-bookings/")
    public ResponseEntity<TourBookingDTO> createBooking(@Valid @RequestBody TourBookingRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourBookingService.create(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/api/tour-bookings/{bookingId}/")
    public ResponseEntity<TourBookingDTO> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(tourBookingService.get(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), bookingId));
    }

    @PostMapping("/api/tour-bookings/{bookingId}/cancel/")
    public ResponseEntity<TourBookingDTO> cancelBooking(@PathVariable("bookingId") UUID bookingId,
                                                        @RequestBody(required = false) CancelBookingRequestDTO body) {
        String reason = body == null ? null : body.getReason();
        return ResponseEntity.ok(tourBookingService.cancel(SecurityUtil.getCurrentUserId(), bookingId, reason));
    }

    /** ?status=all|upcoming|pending|completed|cancelled */
    @GetMapping("/api/me/tour-bookings/")
    public ResponseEntity<List<TourBookingDTO>> getMyBookings(@RequestParam(value = "status", required = false) String status,
                                                              @RequestParam(value = "page", defaultValue = "1") int page,
                                                              @RequestParam(value = "limit", defaultValue = "10") int limit) {
        Page<TourBookingDTO> result = tourBookingService.findMine(SecurityUtil.getCurrentUserId(), status, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    private ResponseEntity<List<TourDTO>> toResponse(Page<TourDTO> result) {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    private int clamp(int limit) {
        return limit < 1 ? 9 : Math.min(limit, MAX_LIMIT);
    }
}
