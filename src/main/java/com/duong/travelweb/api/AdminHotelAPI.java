package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AdminHotelDTO;
import com.duong.travelweb.model.dto.AmenityIdsRequestDTO;
import com.duong.travelweb.model.dto.HotelRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityRequestDTO;
import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.model.dto.RoomRequestDTO;
import com.duong.travelweb.model.dto.RoomTypeDTO;
import com.duong.travelweb.model.dto.RoomTypeRequestDTO;
import com.duong.travelweb.service.AdminHotelService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** API quản trị khách sạn / hạng phòng / phòng. /api/admin/** yêu cầu ROLE_ADMIN (SecurityConfig). */
@RestController
public class AdminHotelAPI {
    private static final int MAX_LIMIT = 100;

    private final AdminHotelService adminHotelService;

    public AdminHotelAPI(AdminHotelService adminHotelService) {
        this.adminHotelService = adminHotelService;
    }

    // ---- Hotels ----

    @GetMapping("/api/admin/hotels/")
    public ResponseEntity<List<AdminHotelDTO>> getHotels(@RequestParam(value = "q", required = false) String keyword,
                                                         @RequestParam(value = "destinationId", required = false) UUID destinationId,
                                                         @RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "page", defaultValue = "1") int page,
                                                         @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Boolean active = switch (status == null ? "" : status) {
            case "active" -> true;
            case "inactive" -> false;
            default -> null;
        };
        List<AdminHotelDTO> results = adminHotelService.findHotels(keyword, destinationId, active, null, page, clamp(limit));
        long total = adminHotelService.countHotels(keyword, destinationId, active, null);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(total)).body(results);
    }

    @PostMapping("/api/admin/hotels/")
    public ResponseEntity<AdminHotelDTO> createHotel(@Valid @RequestBody HotelRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminHotelService.createHotel(request));
    }

    @GetMapping("/api/admin/hotels/{hotelId}/")
    public ResponseEntity<AdminHotelDTO> getHotel(@PathVariable("hotelId") UUID hotelId) {
        return ResponseEntity.ok(adminHotelService.getHotel(hotelId));
    }

    @PutMapping("/api/admin/hotels/{hotelId}/")
    public ResponseEntity<AdminHotelDTO> updateHotel(@PathVariable("hotelId") UUID hotelId,
                                                     @Valid @RequestBody HotelRequestDTO request) {
        return ResponseEntity.ok(adminHotelService.updateHotel(hotelId, request));
    }

    @DeleteMapping("/api/admin/hotels/{hotelId}/")
    public ResponseEntity<Void> deleteHotel(@PathVariable("hotelId") UUID hotelId) {
        adminHotelService.deactivateHotel(hotelId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/hotels/{hotelId}/amenities/")
    public ResponseEntity<AdminHotelDTO> updateHotelAmenities(@PathVariable("hotelId") UUID hotelId,
                                                              @Valid @RequestBody AmenityIdsRequestDTO request) {
        return ResponseEntity.ok(adminHotelService.updateHotelAmenities(hotelId, request.getAmenityIds()));
    }

    // ---- Room types ----

    @GetMapping("/api/admin/hotels/{hotelId}/room-types/")
    public ResponseEntity<List<RoomTypeDTO>> getRoomTypes(@PathVariable("hotelId") UUID hotelId) {
        return ResponseEntity.ok(adminHotelService.findRoomTypes(hotelId));
    }

    @PostMapping("/api/admin/hotels/{hotelId}/room-types/")
    public ResponseEntity<RoomTypeDTO> createRoomType(@PathVariable("hotelId") UUID hotelId,
                                                      @Valid @RequestBody RoomTypeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminHotelService.createRoomType(hotelId, request));
    }

    @PutMapping("/api/admin/hotels/{hotelId}/room-types/{roomTypeId}/")
    public ResponseEntity<RoomTypeDTO> updateRoomType(@PathVariable("hotelId") UUID hotelId,
                                                      @PathVariable("roomTypeId") UUID roomTypeId,
                                                      @Valid @RequestBody RoomTypeRequestDTO request) {
        return ResponseEntity.ok(adminHotelService.updateRoomType(hotelId, roomTypeId, request));
    }

    @DeleteMapping("/api/admin/hotels/{hotelId}/room-types/{roomTypeId}/")
    public ResponseEntity<Void> deleteRoomType(@PathVariable("hotelId") UUID hotelId,
                                               @PathVariable("roomTypeId") UUID roomTypeId) {
        adminHotelService.deactivateRoomType(hotelId, roomTypeId);
        return ResponseEntity.noContent().build();
    }

    // ---- Rooms ----

    @GetMapping("/api/admin/hotels/{hotelId}/rooms/")
    public ResponseEntity<List<RoomDTO>> getRooms(@PathVariable("hotelId") UUID hotelId,
                                                  @RequestParam(value = "roomTypeId", required = false) UUID roomTypeId) {
        return ResponseEntity.ok(adminHotelService.findRooms(hotelId, roomTypeId));
    }

    @PostMapping("/api/admin/hotels/{hotelId}/rooms/")
    public ResponseEntity<RoomDTO> createRoom(@PathVariable("hotelId") UUID hotelId,
                                              @Valid @RequestBody RoomRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminHotelService.createRoom(hotelId, request));
    }

    @GetMapping("/api/admin/hotels/{hotelId}/rooms/{roomId}/")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable("hotelId") UUID hotelId,
                                           @PathVariable("roomId") UUID roomId) {
        return ResponseEntity.ok(adminHotelService.getRoom(hotelId, roomId));
    }

    @PutMapping("/api/admin/hotels/{hotelId}/rooms/{roomId}/")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable("hotelId") UUID hotelId,
                                              @PathVariable("roomId") UUID roomId,
                                              @Valid @RequestBody RoomRequestDTO request) {
        return ResponseEntity.ok(adminHotelService.updateRoom(hotelId, roomId, request));
    }

    @DeleteMapping("/api/admin/hotels/{hotelId}/rooms/{roomId}/")
    public ResponseEntity<Void> deleteRoom(@PathVariable("hotelId") UUID hotelId,
                                           @PathVariable("roomId") UUID roomId) {
        adminHotelService.deleteRoom(hotelId, roomId);
        return ResponseEntity.noContent().build();
    }

    // ---- Room availability ----

    @GetMapping("/api/admin/rooms/{roomId}/availability/")
    public ResponseEntity<List<RoomAvailabilityDTO>> getRoomAvailability(
            @PathVariable("roomId") UUID roomId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminHotelService.getRoomAvailability(roomId, from, to));
    }

    @PutMapping("/api/admin/rooms/{roomId}/availability/")
    public ResponseEntity<List<RoomAvailabilityDTO>> updateRoomAvailability(@PathVariable("roomId") UUID roomId,
                                                                            @Valid @RequestBody RoomAvailabilityRequestDTO request) {
        return ResponseEntity.ok(adminHotelService.updateRoomAvailability(roomId, request));
    }

    private int clamp(int limit) {
        return limit < 1 ? 20 : Math.min(limit, MAX_LIMIT);
    }
}
