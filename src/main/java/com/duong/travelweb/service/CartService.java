package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.CartCheckoutRequestDTO;
import com.duong.travelweb.model.dto.CartDTO;
import com.duong.travelweb.model.dto.CartItemRequestDTO;

import java.util.UUID;

/**
 * Giỏ hàng khách sạn (hiện chỉ hỗ trợ hotel). Giá / tình trạng phòng luôn tính lại khi đọc;
 * checkout dùng giá hiện tại.
 * <p>
 * Khách chưa đăng nhập cũng có giỏ (guest cart): server cấp token ngẫu nhiên ở lần thêm dòng đầu tiên
 * ({@link CartDTO#getGuestToken()}), client gửi lại qua header X-Cart-Token; DB chỉ lưu SHA-256 của token.
 * Đăng nhập xong gọi {@link #merge} để gộp vào giỏ của user. Checkout bắt buộc đăng nhập.
 */
public interface CartService {
    /** Chủ giỏ: user đăng nhập (ưu tiên) hoặc token giỏ khách. */
    record CartOwner(UUID userId, String guestToken) {
        public boolean isGuest() {
            return userId == null;
        }
    }

    CartDTO getCart(CartOwner owner);

    /** Cùng loại phòng + cùng ngày đã có trong giỏ -> cộng dồn số phòng và số khách. */
    CartDTO addItem(CartOwner owner, CartItemRequestDTO request);

    CartDTO updateItem(CartOwner owner, UUID itemId, CartItemRequestDTO request);

    CartDTO removeItem(CartOwner owner, UUID itemId);

    void clear(CartOwner owner);

    /** Tạo 1 order cho các dòng được chọn rồi xoá chúng khỏi giỏ. */
    CartCheckoutDTO checkout(UUID userId, CartCheckoutRequestDTO request);

    /**
     * Gộp giỏ khách (theo token) vào giỏ của user rồi xoá giỏ khách. Dòng trùng loại phòng + ngày được cộng dồn
     * nếu vẫn hợp lệ (còn phòng, ≤ 5 phòng), không thì giữ dòng của user; quá 20 dòng thì bỏ phần dư.
     * Token không tồn tại / rỗng -> chỉ trả giỏ của user (idempotent).
     */
    CartDTO merge(UUID userId, String guestToken);

    /** Xoá giỏ khách không hoạt động quá {@code days} ngày; trả số giỏ đã xoá. */
    int purgeStaleGuestCarts(int days);
}
