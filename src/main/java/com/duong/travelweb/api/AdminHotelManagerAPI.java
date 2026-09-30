package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.HotelAssignmentRequestDTO;
import com.duong.travelweb.model.dto.HotelManagerDTO;
import com.duong.travelweb.model.dto.HotelManagerRequestDTO;
import com.duong.travelweb.service.HotelManagerAccountService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API quản trị tài khoản quản lý khách sạn. */
@RestController
public class AdminHotelManagerAPI {
    private final HotelManagerAccountService hotelManagerAccountService;

    public AdminHotelManagerAPI(HotelManagerAccountService hotelManagerAccountService) {
        this.hotelManagerAccountService = hotelManagerAccountService;
    }

    @GetMapping("/api/admin/hotel-managers/")
    public ResponseEntity<List<HotelManagerDTO>> getManagers() {
        return ResponseEntity.ok(hotelManagerAccountService.findManagers());
    }

    @PostMapping("/api/admin/hotel-managers/")
    public ResponseEntity<HotelManagerDTO> createManager(@Valid @RequestBody HotelManagerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hotelManagerAccountService.createManager(SecurityUtil.getCurrentUserId(), request));
    }

    @PutMapping("/api/admin/hotel-managers/{managerId}/hotels/")
    public ResponseEntity<HotelManagerDTO> assignHotels(@PathVariable("managerId") UUID managerId,
                                                         @Valid @RequestBody HotelAssignmentRequestDTO request) {
        return ResponseEntity.ok(hotelManagerAccountService.assignHotels(managerId, request));
    }
}
