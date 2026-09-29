package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.CartCheckoutRequestDTO;
import com.duong.travelweb.model.dto.CartDTO;
import com.duong.travelweb.model.dto.CartItemRequestDTO;
import com.duong.travelweb.service.CartService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Giỏ hàng của user đang đăng nhập (yêu cầu đăng nhập). */
@RestController
public class CartAPI {
    private final CartService cartService;

    public CartAPI(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/api/cart/")
    public ResponseEntity<CartDTO> getCart() {
        return ResponseEntity.ok(cartService.getCart(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/api/cart/items/")
    public ResponseEntity<CartDTO> addItem(@Valid @RequestBody CartItemRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(SecurityUtil.getCurrentUserId(), request));
    }

    @PutMapping("/api/cart/items/{itemId}/")
    public ResponseEntity<CartDTO> updateItem(@PathVariable("itemId") UUID itemId,
                                               @Valid @RequestBody CartItemRequestDTO request) {
        return ResponseEntity.ok(cartService.updateItem(SecurityUtil.getCurrentUserId(), itemId, request));
    }

    @DeleteMapping("/api/cart/items/{itemId}/")
    public ResponseEntity<CartDTO> removeItem(@PathVariable("itemId") UUID itemId) {
        return ResponseEntity.ok(cartService.removeItem(SecurityUtil.getCurrentUserId(), itemId));
    }

    @DeleteMapping("/api/cart/clear/")
    public ResponseEntity<Void> clear() {
        cartService.clear(SecurityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/cart/checkout/")
    public ResponseEntity<CartCheckoutDTO> checkout(@Valid @RequestBody CartCheckoutRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.checkout(SecurityUtil.getCurrentUserId(), request));
    }
}
