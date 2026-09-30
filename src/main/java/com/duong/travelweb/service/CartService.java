package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.CartCheckoutRequestDTO;
import com.duong.travelweb.model.dto.CartDTO;
import com.duong.travelweb.model.dto.CartItemRequestDTO;

import java.util.UUID;

/**
 * Giỏ hàng của user đăng nhập (hiện chỉ hỗ trợ hotel). Giá / tình trạng phòng luôn tính lại khi đọc;
 * checkout dùng giá hiện tại.
 */
public interface CartService {
    CartDTO getCart(UUID userId);

    /** Cùng loại phòng + cùng ngày đã có trong giỏ -> cộng dồn số phòng và số khách. */
    CartDTO addItem(UUID userId, CartItemRequestDTO request);

    CartDTO updateItem(UUID userId, UUID itemId, CartItemRequestDTO request);

    CartDTO removeItem(UUID userId, UUID itemId);

    void clear(UUID userId);

    /** Tạo 1 order cho các dòng được chọn rồi xoá chúng khỏi giỏ. */
    CartCheckoutDTO checkout(UUID userId, CartCheckoutRequestDTO request);
}
