package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.CartCheckoutRequestDTO;
import com.duong.travelweb.model.dto.CartDTO;
import com.duong.travelweb.model.dto.CartItemRequestDTO;
import com.duong.travelweb.service.CartService;
import com.duong.travelweb.service.CartService.CartOwner;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Giỏ hàng khách sạn. Xem / thêm / sửa / xoá được cả khi chưa đăng nhập (giỏ khách, header X-Cart-Token);
 * checkout và gộp giỏ yêu cầu đăng nhập.
 */
@RestController
public class CartAPI {
    public static final String CART_TOKEN_HEADER = "X-Cart-Token";

    private final CartService cartService;

    public CartAPI(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/api/cart/")
    public ResponseEntity<CartDTO> getCart(@RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken) {
        return ResponseEntity.ok(cartService.getCart(owner(cartToken)));
    }

    @PostMapping("/api/cart/items/")
    public ResponseEntity<CartDTO> addItem(@RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken,
                                           @Valid @RequestBody CartItemRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(owner(cartToken), request));
    }

    @PutMapping("/api/cart/items/{itemId}/")
    public ResponseEntity<CartDTO> updateItem(@RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken,
                                              @PathVariable("itemId") UUID itemId,
                                              @Valid @RequestBody CartItemRequestDTO request) {
        return ResponseEntity.ok(cartService.updateItem(owner(cartToken), itemId, request));
    }

    @DeleteMapping("/api/cart/items/{itemId}/")
    public ResponseEntity<CartDTO> removeItem(@RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken,
                                              @PathVariable("itemId") UUID itemId) {
        return ResponseEntity.ok(cartService.removeItem(owner(cartToken), itemId));
    }

    @DeleteMapping("/api/cart/clear/")
    public ResponseEntity<Void> clear(@RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken) {
        cartService.clear(owner(cartToken));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/cart/checkout/")
    public ResponseEntity<CartCheckoutDTO> checkout(@Valid @RequestBody CartCheckoutRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.checkout(SecurityUtil.getCurrentUserId(), request));
    }

    /** Gộp giỏ khách (header X-Cart-Token) vào giỏ của user đang đăng nhập; client xoá token sau khi gọi. */
    @PostMapping("/api/cart/merge/")
    public ResponseEntity<CartDTO> merge(@RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken) {
        return ResponseEntity.ok(cartService.merge(SecurityUtil.getCurrentUserId(), cartToken));
    }

    /** Đã đăng nhập thì luôn dùng giỏ của user (bỏ qua token khách cho tới khi gộp). */
    private CartOwner owner(String cartToken) {
        UUID userId = SecurityUtil.findCurrentUserId();
        return userId != null ? new CartOwner(userId, null) : new CartOwner(null, cartToken);
    }
}
