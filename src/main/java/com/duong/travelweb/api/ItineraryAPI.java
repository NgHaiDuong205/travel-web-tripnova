package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.ItineraryDTO;
import com.duong.travelweb.model.dto.ItineraryItemRequestDTO;
import com.duong.travelweb.model.dto.ItineraryRequestDTO;
import com.duong.travelweb.service.ItineraryService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Lịch trình cá nhân của user đang đăng nhập (/api/me/** yêu cầu đăng nhập). */
@RestController
public class ItineraryAPI {
    private static final int MAX_LIMIT = 100;

    private final ItineraryService itineraryService;

    public ItineraryAPI(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @GetMapping("/api/me/itineraries/")
    public ResponseEntity<List<ItineraryDTO>> getItineraries(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        Page<ItineraryDTO> result = itineraryService.findMine(SecurityUtil.getCurrentUserId(), page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @PostMapping("/api/me/itineraries/")
    public ResponseEntity<ItineraryDTO> createItinerary(@Valid @RequestBody ItineraryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.create(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/api/me/itineraries/{itineraryId}/")
    public ResponseEntity<ItineraryDTO> getItinerary(@PathVariable("itineraryId") UUID itineraryId) {
        return ResponseEntity.ok(itineraryService.getMine(SecurityUtil.getCurrentUserId(), itineraryId));
    }

    @PutMapping("/api/me/itineraries/{itineraryId}/")
    public ResponseEntity<ItineraryDTO> updateItinerary(@PathVariable("itineraryId") UUID itineraryId,
                                                          @Valid @RequestBody ItineraryRequestDTO request) {
        return ResponseEntity.ok(itineraryService.update(SecurityUtil.getCurrentUserId(), itineraryId, request));
    }

    @DeleteMapping("/api/me/itineraries/{itineraryId}/")
    public ResponseEntity<Void> deleteItinerary(@PathVariable("itineraryId") UUID itineraryId) {
        itineraryService.delete(SecurityUtil.getCurrentUserId(), itineraryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/me/itineraries/{itineraryId}/items/")
    public ResponseEntity<ItineraryDTO> addItem(@PathVariable("itineraryId") UUID itineraryId,
                                                  @Valid @RequestBody ItineraryItemRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.addItem(SecurityUtil.getCurrentUserId(), itineraryId, request));
    }

    @DeleteMapping("/api/me/itineraries/{itineraryId}/items/{itemId}/")
    public ResponseEntity<Void> deleteItem(@PathVariable("itineraryId") UUID itineraryId,
                                            @PathVariable("itemId") UUID itemId) {
        itineraryService.deleteItem(SecurityUtil.getCurrentUserId(), itineraryId, itemId);
        return ResponseEntity.noContent().build();
    }

    private int clamp(int limit) {
        return limit < 1 ? 20 : Math.min(limit, MAX_LIMIT);
    }
}
