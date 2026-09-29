package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.TripCartCheckoutRequestDTO;
import com.duong.travelweb.model.dto.TripCartItemDTO;
import com.duong.travelweb.model.dto.TripCartItemRequestDTO;
import com.duong.travelweb.service.TripCartService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Giỏ chuyến đi (tour / xe / chuyến bay) của user đăng nhập — giỏ khách sạn vẫn ở /api/cart. */
@RestController
public class TripCartAPI {
    private final TripCartService tripCartService;

    public TripCartAPI(TripCartService tripCartService) {
        this.tripCartService = tripCartService;
    }

    @GetMapping("/api/trip-cart/")
    public ResponseEntity<List<TripCartItemDTO>> getItems() {
        return ResponseEntity.ok(tripCartService.getItems(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/api/trip-cart/items/")
    public ResponseEntity<List<TripCartItemDTO>> addItem(@Valid @RequestBody TripCartItemRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripCartService.addItem(SecurityUtil.getCurrentUserId(), request));
    }

    @DeleteMapping("/api/trip-cart/items/{itemId}/")
    public ResponseEntity<List<TripCartItemDTO>> removeItem(@PathVariable("itemId") UUID itemId) {
        return ResponseEntity.ok(tripCartService.removeItem(SecurityUtil.getCurrentUserId(), itemId));
    }

    @PostMapping("/api/trip-cart/checkout/")
    public ResponseEntity<CartCheckoutDTO> checkout(@Valid @RequestBody TripCartCheckoutRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripCartService.checkout(SecurityUtil.getCurrentUserId(), request));
    }
}
