package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.HotelImageDTO;
import com.duong.travelweb.model.dto.HotelImageRequestDTO;
import com.duong.travelweb.service.HotelImageService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Thư viện ảnh khách sạn: xem công khai, quản trị ở /api/admin/** (ROLE_ADMIN). */
@RestController
public class HotelImageAPI {
    private final HotelImageService hotelImageService;

    public HotelImageAPI(HotelImageService hotelImageService) {
        this.hotelImageService = hotelImageService;
    }

    @GetMapping("/api/hotels/{hotelId}/images/")
    public ResponseEntity<List<HotelImageDTO>> getImages(@PathVariable("hotelId") UUID hotelId) {
        return ResponseEntity.ok(hotelImageService.findByHotel(hotelId, true));
    }

    @GetMapping("/api/admin/hotels/{hotelId}/images/")
    public ResponseEntity<List<HotelImageDTO>> getImagesForAdmin(@PathVariable("hotelId") UUID hotelId) {
        return ResponseEntity.ok(hotelImageService.findByHotel(hotelId, false));
    }

    @PostMapping("/api/admin/hotels/{hotelId}/images/")
    public ResponseEntity<HotelImageDTO> addImage(@PathVariable("hotelId") UUID hotelId,
                                                  @Valid @RequestBody HotelImageRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hotelImageService.add(SecurityUtil.getCurrentUserId(), hotelId, request));
    }

    @PutMapping("/api/admin/hotels/{hotelId}/images/{imageId}/")
    public ResponseEntity<HotelImageDTO> updateImage(@PathVariable("hotelId") UUID hotelId,
                                                     @PathVariable("imageId") UUID imageId,
                                                     @Valid @RequestBody HotelImageRequestDTO request) {
        return ResponseEntity.ok(hotelImageService.update(hotelId, imageId, request));
    }

    @PostMapping("/api/admin/hotels/{hotelId}/images/{imageId}/cover/")
    public ResponseEntity<HotelImageDTO> setCover(@PathVariable("hotelId") UUID hotelId,
                                                  @PathVariable("imageId") UUID imageId) {
        return ResponseEntity.ok(hotelImageService.setCover(hotelId, imageId));
    }

    @DeleteMapping("/api/admin/hotels/{hotelId}/images/{imageId}/")
    public ResponseEntity<Void> deleteImage(@PathVariable("hotelId") UUID hotelId,
                                            @PathVariable("imageId") UUID imageId) {
        hotelImageService.delete(hotelId, imageId);
        return ResponseEntity.noContent().build();
    }
}
