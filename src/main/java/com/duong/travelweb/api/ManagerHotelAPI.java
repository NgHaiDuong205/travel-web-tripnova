package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AdminHotelDTO;
import com.duong.travelweb.model.dto.AdminStatsDTO;
import com.duong.travelweb.model.dto.AmenityIdsRequestDTO;
import com.duong.travelweb.model.dto.HotelImageDTO;
import com.duong.travelweb.model.dto.HotelImageRequestDTO;
import com.duong.travelweb.model.dto.HotelRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityRequestDTO;
import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.model.dto.RoomRequestDTO;
import com.duong.travelweb.model.dto.RoomTypeDTO;
import com.duong.travelweb.model.dto.RoomTypeRequestDTO;
import com.duong.travelweb.service.ManagedHotelService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Trang quản lý khách sạn (ROLE_HOTEL_MANAGER qua /api/manager/**); chỉ khách sạn mình quản lý. */
@RestController
public class ManagerHotelAPI {
    private final ManagedHotelService managedHotelService;

    public ManagerHotelAPI(ManagedHotelService managedHotelService) {
        this.managedHotelService = managedHotelService;
    }

    @GetMapping("/api/manager/hotels/")
    public ResponseEntity<List<AdminHotelDTO>> getHotels() {
        return ResponseEntity.ok(managedHotelService.findHotels(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/api/manager/hotels/{hotelId}/")
    public ResponseEntity<AdminHotelDTO> getHotel(@PathVariable("hotelId") UUID hotelId) {
        return ResponseEntity.ok(managedHotelService.getHotel(SecurityUtil.getCurrentUserId(), hotelId));
    }

    @PutMapping("/api/manager/hotels/{hotelId}/")
    public ResponseEntity<AdminHotelDTO> updateHotel(@PathVariable("hotelId") UUID hotelId,
                                                      @Valid @RequestBody HotelRequestDTO request) {
        return ResponseEntity.ok(managedHotelService.updateHotel(SecurityUtil.getCurrentUserId(), hotelId, request));
    }

    @DeleteMapping("/api/manager/hotels/{hotelId}/")
    public ResponseEntity<Void> deleteHotel(@PathVariable("hotelId") UUID hotelId) {
        managedHotelService.deactivateHotel(SecurityUtil.getCurrentUserId(), hotelId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/manager/hotels/{hotelId}/amenities/")
    public ResponseEntity<AdminHotelDTO> updateHotelAmenities(@PathVariable("hotelId") UUID hotelId,
                                                               @Valid @RequestBody AmenityIdsRequestDTO request) {
        return ResponseEntity.ok(managedHotelService.updateHotelAmenities(
                SecurityUtil.getCurrentUserId(), hotelId, request.getAmenityIds()));
    }

    @GetMapping("/api/manager/hotels/{hotelId}/room-types/")
    public ResponseEntity<List<RoomTypeDTO>> getRoomTypes(@PathVariable("hotelId") UUID hotelId) {
        return ResponseEntity.ok(managedHotelService.findRoomTypes(SecurityUtil.getCurrentUserId(), hotelId));
    }

    @PostMapping("/api/manager/hotels/{hotelId}/room-types/")
    public ResponseEntity<RoomTypeDTO> createRoomType(@PathVariable("hotelId") UUID hotelId,
                                                       @Valid @RequestBody RoomTypeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managedHotelService.createRoomType(SecurityUtil.getCurrentUserId(), hotelId, request));
    }

    @PutMapping("/api/manager/hotels/{hotelId}/room-types/{roomTypeId}/")
    public ResponseEntity<RoomTypeDTO> updateRoomType(@PathVariable("hotelId") UUID hotelId,
                                                       @PathVariable("roomTypeId") UUID roomTypeId,
                                                       @Valid @RequestBody RoomTypeRequestDTO request) {
        return ResponseEntity.ok(managedHotelService.updateRoomType(
                SecurityUtil.getCurrentUserId(), hotelId, roomTypeId, request));
    }

    @DeleteMapping("/api/manager/hotels/{hotelId}/room-types/{roomTypeId}/")
    public ResponseEntity<Void> deleteRoomType(@PathVariable("hotelId") UUID hotelId,
                                                @PathVariable("roomTypeId") UUID roomTypeId) {
        managedHotelService.deactivateRoomType(SecurityUtil.getCurrentUserId(), hotelId, roomTypeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/manager/hotels/{hotelId}/rooms/")
    public ResponseEntity<List<RoomDTO>> getRooms(@PathVariable("hotelId") UUID hotelId,
                                                   @RequestParam(value = "roomTypeId", required = false) UUID roomTypeId) {
        return ResponseEntity.ok(managedHotelService.findRooms(SecurityUtil.getCurrentUserId(), hotelId, roomTypeId));
    }

    @PostMapping("/api/manager/hotels/{hotelId}/rooms/")
    public ResponseEntity<RoomDTO> createRoom(@PathVariable("hotelId") UUID hotelId,
                                               @Valid @RequestBody RoomRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managedHotelService.createRoom(SecurityUtil.getCurrentUserId(), hotelId, request));
    }

    @GetMapping("/api/manager/hotels/{hotelId}/rooms/{roomId}/")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable("hotelId") UUID hotelId,
                                            @PathVariable("roomId") UUID roomId) {
        return ResponseEntity.ok(managedHotelService.getRoom(SecurityUtil.getCurrentUserId(), hotelId, roomId));
    }

    @PutMapping("/api/manager/hotels/{hotelId}/rooms/{roomId}/")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable("hotelId") UUID hotelId,
                                               @PathVariable("roomId") UUID roomId,
                                               @Valid @RequestBody RoomRequestDTO request) {
        return ResponseEntity.ok(managedHotelService.updateRoom(SecurityUtil.getCurrentUserId(), hotelId, roomId, request));
    }

    @DeleteMapping("/api/manager/hotels/{hotelId}/rooms/{roomId}/")
    public ResponseEntity<Void> deleteRoom(@PathVariable("hotelId") UUID hotelId,
                                            @PathVariable("roomId") UUID roomId) {
        managedHotelService.deleteRoom(SecurityUtil.getCurrentUserId(), hotelId, roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/manager/rooms/{roomId}/availability/")
    public ResponseEntity<List<RoomAvailabilityDTO>> getRoomAvailability(
            @PathVariable("roomId") UUID roomId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(managedHotelService.getRoomAvailability(SecurityUtil.getCurrentUserId(), roomId, from, to));
    }

    @PutMapping("/api/manager/rooms/{roomId}/availability/")
    public ResponseEntity<List<RoomAvailabilityDTO>> updateRoomAvailability(@PathVariable("roomId") UUID roomId,
                                                                             @Valid @RequestBody RoomAvailabilityRequestDTO request) {
        return ResponseEntity.ok(managedHotelService.updateRoomAvailability(SecurityUtil.getCurrentUserId(), roomId, request));
    }

    @GetMapping("/api/manager/hotels/{hotelId}/images/")
    public ResponseEntity<List<HotelImageDTO>> getImages(@PathVariable("hotelId") UUID hotelId) {
        return ResponseEntity.ok(managedHotelService.findImages(SecurityUtil.getCurrentUserId(), hotelId));
    }

    @PostMapping("/api/manager/hotels/{hotelId}/images/")
    public ResponseEntity<HotelImageDTO> addImage(@PathVariable("hotelId") UUID hotelId,
                                                   @Valid @RequestBody HotelImageRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managedHotelService.addImage(SecurityUtil.getCurrentUserId(), hotelId, request));
    }

    @PutMapping("/api/manager/hotels/{hotelId}/images/{imageId}/")
    public ResponseEntity<HotelImageDTO> updateImage(@PathVariable("hotelId") UUID hotelId,
                                                      @PathVariable("imageId") UUID imageId,
                                                      @Valid @RequestBody HotelImageRequestDTO request) {
        return ResponseEntity.ok(managedHotelService.updateImage(SecurityUtil.getCurrentUserId(), hotelId, imageId, request));
    }

    @PostMapping("/api/manager/hotels/{hotelId}/images/{imageId}/cover/")
    public ResponseEntity<HotelImageDTO> setCover(@PathVariable("hotelId") UUID hotelId,
                                                   @PathVariable("imageId") UUID imageId) {
        return ResponseEntity.ok(managedHotelService.setCoverImage(SecurityUtil.getCurrentUserId(), hotelId, imageId));
    }

    @DeleteMapping("/api/manager/hotels/{hotelId}/images/{imageId}/")
    public ResponseEntity<Void> deleteImage(@PathVariable("hotelId") UUID hotelId,
                                             @PathVariable("imageId") UUID imageId) {
        managedHotelService.deleteImage(SecurityUtil.getCurrentUserId(), hotelId, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/manager/stats/")
    public ResponseEntity<AdminStatsDTO> getStats(
            @RequestParam(value = "hotelId", required = false) UUID hotelId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "granularity", required = false) String granularity) {
        return ResponseEntity.ok(managedHotelService.getStats(
                SecurityUtil.getCurrentUserId(), hotelId, from, to, granularity));
    }
}
