package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.RoomTypeImageDTO;
import com.duong.travelweb.model.dto.RoomTypeImageRequestDTO;
import com.duong.travelweb.service.RoomTypeImageService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Thư viện ảnh hạng phòng: xem công khai, quản trị ở /api/admin/** (ROLE_ADMIN). Manager: ManagerHotelAPI. */
@RestController
public class RoomTypeImageAPI {
    private final RoomTypeImageService roomTypeImageService;

    public RoomTypeImageAPI(RoomTypeImageService roomTypeImageService) {
        this.roomTypeImageService = roomTypeImageService;
    }

    @GetMapping("/api/hotels/{hotelId}/room-types/{roomTypeId}/images/")
    public ResponseEntity<List<RoomTypeImageDTO>> getImages(@PathVariable("hotelId") UUID hotelId,
                                                            @PathVariable("roomTypeId") UUID roomTypeId) {
        return ResponseEntity.ok(roomTypeImageService.findByRoomType(hotelId, roomTypeId, true));
    }

    @GetMapping("/api/admin/hotels/{hotelId}/room-types/{roomTypeId}/images/")
    public ResponseEntity<List<RoomTypeImageDTO>> getImagesForAdmin(@PathVariable("hotelId") UUID hotelId,
                                                                    @PathVariable("roomTypeId") UUID roomTypeId) {
        return ResponseEntity.ok(roomTypeImageService.findByRoomType(hotelId, roomTypeId, false));
    }

    @PostMapping("/api/admin/hotels/{hotelId}/room-types/{roomTypeId}/images/")
    public ResponseEntity<RoomTypeImageDTO> addImage(@PathVariable("hotelId") UUID hotelId,
                                                     @PathVariable("roomTypeId") UUID roomTypeId,
                                                     @Valid @RequestBody RoomTypeImageRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomTypeImageService.add(SecurityUtil.getCurrentUserId(), hotelId, roomTypeId, request));
    }

    @PutMapping("/api/admin/hotels/{hotelId}/room-types/{roomTypeId}/images/{imageId}/")
    public ResponseEntity<RoomTypeImageDTO> updateImage(@PathVariable("hotelId") UUID hotelId,
                                                        @PathVariable("roomTypeId") UUID roomTypeId,
                                                        @PathVariable("imageId") UUID imageId,
                                                        @Valid @RequestBody RoomTypeImageRequestDTO request) {
        return ResponseEntity.ok(roomTypeImageService.update(hotelId, roomTypeId, imageId, request));
    }

    @PostMapping("/api/admin/hotels/{hotelId}/room-types/{roomTypeId}/images/{imageId}/cover/")
    public ResponseEntity<RoomTypeImageDTO> setCover(@PathVariable("hotelId") UUID hotelId,
                                                     @PathVariable("roomTypeId") UUID roomTypeId,
                                                     @PathVariable("imageId") UUID imageId) {
        return ResponseEntity.ok(roomTypeImageService.setCover(hotelId, roomTypeId, imageId));
    }

    @DeleteMapping("/api/admin/hotels/{hotelId}/room-types/{roomTypeId}/images/{imageId}/")
    public ResponseEntity<Void> deleteImage(@PathVariable("hotelId") UUID hotelId,
                                            @PathVariable("roomTypeId") UUID roomTypeId,
                                            @PathVariable("imageId") UUID imageId) {
        roomTypeImageService.delete(hotelId, roomTypeId, imageId);
        return ResponseEntity.noContent().build();
    }
}
