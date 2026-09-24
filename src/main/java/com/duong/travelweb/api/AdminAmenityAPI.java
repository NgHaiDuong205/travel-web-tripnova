package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AdminAmenityDTO;
import com.duong.travelweb.model.dto.AmenityRequestDTO;
import com.duong.travelweb.service.AmenityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API quản trị tiện ích (master data). /api/admin/** yêu cầu ROLE_ADMIN (SecurityConfig). */
@RestController
public class AdminAmenityAPI {
    private final AmenityService amenityService;

    public AdminAmenityAPI(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @GetMapping("/api/admin/amenities/")
    public ResponseEntity<List<AdminAmenityDTO>> getAmenities(@RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(amenityService.findForAdmin(category));
    }

    @PostMapping("/api/admin/amenities/")
    public ResponseEntity<AdminAmenityDTO> createAmenity(@Valid @RequestBody AmenityRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amenityService.create(request));
    }

    @PutMapping("/api/admin/amenities/{amenityId}/")
    public ResponseEntity<AdminAmenityDTO> updateAmenity(@PathVariable("amenityId") UUID amenityId,
                                                         @Valid @RequestBody AmenityRequestDTO request) {
        return ResponseEntity.ok(amenityService.update(amenityId, request));
    }

    /** ?force=true để xoá cả khi đang được dùng (gỡ khỏi mọi khách sạn / hạng phòng). */
    @DeleteMapping("/api/admin/amenities/{amenityId}/")
    public ResponseEntity<Void> deleteAmenity(@PathVariable("amenityId") UUID amenityId,
                                              @RequestParam(value = "force", defaultValue = "false") boolean force) {
        amenityService.delete(amenityId, force);
        return ResponseEntity.noContent().build();
    }
}
