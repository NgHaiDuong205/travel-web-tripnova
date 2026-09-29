package com.duong.travelweb.api;

import com.duong.travelweb.builder.FlightSearchBuilder;
import com.duong.travelweb.model.dto.AirportDTO;
import com.duong.travelweb.model.dto.CancelBookingRequestDTO;
import com.duong.travelweb.model.dto.FlightBookingDTO;
import com.duong.travelweb.model.dto.FlightBookingRequestDTO;
import com.duong.travelweb.model.dto.FlightDTO;
import com.duong.travelweb.model.dto.FlightFilterOptionsDTO;
import com.duong.travelweb.model.dto.FlightOrderCreatedDTO;
import com.duong.travelweb.model.dto.FlightSeatDTO;
import com.duong.travelweb.model.dto.SelectSeatRequestDTO;
import com.duong.travelweb.service.FlightBookingService;
import com.duong.travelweb.service.FlightService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Chuyến bay / sân bay (GET công khai) và đặt vé (/api/flight-bookings/**, /api/me/flight-bookings/ — cần đăng nhập). */
@RestController
public class FlightAPI {
    private static final int MAX_LIMIT = 100;

    private final FlightService flightService;
    private final FlightBookingService flightBookingService;

    public FlightAPI(FlightService flightService, FlightBookingService flightBookingService) {
        this.flightService = flightService;
        this.flightBookingService = flightBookingService;
    }

    /**
     * ?from&to (mã sân bay hoặc tên thành phố)&departDate (yyyy-MM-dd)&class&pax&airlines=a,b&priceMax
     * &time=morning|afternoon|evening|night&sort=departure|price_asc|price_desc|duration&page&limit
     */
    @GetMapping("/api/flights/")
    public ResponseEntity<List<FlightDTO>> getFlights(@RequestParam(value = "from", required = false) String from,
                                                      @RequestParam(value = "to", required = false) String to,
                                                      @RequestParam(value = "departDate", required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departDate,
                                                      @RequestParam(value = "class", required = false) String seatClass,
                                                      @RequestParam(value = "pax", required = false) Integer pax,
                                                      @RequestParam(value = "airlines", required = false) String airlines,
                                                      @RequestParam(value = "priceMax", required = false) BigDecimal priceMax,
                                                      @RequestParam(value = "time", required = false) String time,
                                                      @RequestParam(value = "sort", required = false) String sort,
                                                      @RequestParam(value = "page", defaultValue = "1") int page,
                                                      @RequestParam(value = "limit", defaultValue = "10") int limit) {
        FlightSearchBuilder criteria = new FlightSearchBuilder.Builder()
                .upcomingActiveOnly(true)
                .from(blankToNull(from))
                .to(blankToNull(to))
                .departDate(departDate)
                .seatClass(blankToNull(seatClass))
                .passengers(pax)
                .airlines(splitList(airlines))
                .priceMax(priceMax)
                .timeOfDay(blankToNull(time))
                .sort(sort)
                .build();
        return toResponse(flightService.search(criteria, page, clamp(limit)));
    }

    @GetMapping("/api/flights/filters/")
    public ResponseEntity<FlightFilterOptionsDTO> getFilters() {
        return ResponseEntity.ok(flightService.filterOptions());
    }

    @GetMapping("/api/flights/{flightId}/")
    public ResponseEntity<FlightDTO> getFlight(@PathVariable("flightId") UUID flightId) {
        return ResponseEntity.ok(flightService.getPublic(flightId));
    }

    /** Sơ đồ ghế: status available | occupied | blocked. ?class để lọc hạng. */
    @GetMapping("/api/flights/{flightId}/seats/")
    public ResponseEntity<List<FlightSeatDTO>> getSeats(@PathVariable("flightId") UUID flightId,
                                                        @RequestParam(value = "class", required = false) String seatClass) {
        return ResponseEntity.ok(flightService.seats(flightId, blankToNull(seatClass), false));
    }

    /** Sân bay có chuyến đang bán, ?q = mã hoặc tên thành phố (autocomplete). */
    @GetMapping("/api/airports/")
    public ResponseEntity<List<AirportDTO>> getAirports(@RequestParam(value = "q", required = false) String q) {
        return ResponseEntity.ok(flightService.airports(q));
    }

    /** Tạo order (pending, giữ ghế hold-minutes) cho các hành khách; FE chuyển tới /trip-payment/{paymentId}. */
    @PostMapping("/api/flight-bookings/")
    public ResponseEntity<FlightOrderCreatedDTO> createBooking(@Valid @RequestBody FlightBookingRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flightBookingService.create(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/api/flight-bookings/{bookingId}/")
    public ResponseEntity<FlightBookingDTO> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(flightBookingService.get(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), bookingId));
    }

    @PostMapping("/api/flight-bookings/{bookingId}/select-seat/")
    public ResponseEntity<FlightBookingDTO> selectSeat(@PathVariable("bookingId") UUID bookingId,
                                                       @Valid @RequestBody SelectSeatRequestDTO request) {
        return ResponseEntity.ok(flightBookingService.selectSeat(SecurityUtil.getCurrentUserId(), bookingId, request.getSeatId()));
    }

    @PostMapping("/api/flight-bookings/{bookingId}/cancel/")
    public ResponseEntity<FlightBookingDTO> cancelBooking(@PathVariable("bookingId") UUID bookingId,
                                                          @RequestBody(required = false) CancelBookingRequestDTO body) {
        String reason = body == null ? null : body.getReason();
        return ResponseEntity.ok(flightBookingService.cancel(SecurityUtil.getCurrentUserId(), bookingId, reason));
    }

    /** ?status=all|upcoming|pending|completed|cancelled */
    @GetMapping("/api/me/flight-bookings/")
    public ResponseEntity<List<FlightBookingDTO>> getMyBookings(@RequestParam(value = "status", required = false) String status,
                                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                                @RequestParam(value = "limit", defaultValue = "10") int limit) {
        Page<FlightBookingDTO> result = flightBookingService.findMine(SecurityUtil.getCurrentUserId(), status, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    private ResponseEntity<List<FlightDTO>> toResponse(Page<FlightDTO> result) {
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
        return limit < 1 ? 10 : Math.min(limit, MAX_LIMIT);
    }
}
