package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.TripCartCheckoutRequestDTO;
import com.duong.travelweb.model.dto.TripCartItemDTO;
import com.duong.travelweb.model.dto.TripCartItemRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Giỏ chuyến đi: tour / xe / chuyến bay (dùng chung bảng carts, cart_items với giỏ khách sạn nhưng tách API).
 * Không giữ chỗ khi còn trong giỏ; mỗi lần đọc tính lại giá và kiểm tra còn đặt được không.
 * Thanh toán theo từng loại vì mỗi order chỉ chứa một loại booking.
 */
public interface TripCartService {
    List<TripCartItemDTO> getItems(UUID userId);

    /** Kiểm tra như khi đặt thật (ngày, còn chỗ, ghế...) rồi mới thêm. */
    List<TripCartItemDTO> addItem(UUID userId, TripCartItemRequestDTO request);

    List<TripCartItemDTO> removeItem(UUID userId, UUID itemId);

    /** Tạo 1 order + 1 payment cho các dòng của itemType (hoặc itemIds) rồi xoá chúng khỏi giỏ; lỗi -> giỏ giữ nguyên. */
    CartCheckoutDTO checkout(UUID userId, TripCartCheckoutRequestDTO request);
}
