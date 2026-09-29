package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * Thêm tour / xe / chuyến bay vào giỏ. booking = đúng body của POST /api/tour-bookings, /api/car-bookings hoặc
 * /api/flight-bookings (không cần paymentMethod — chọn khi thanh toán giỏ).
 */
public class TripCartItemRequestDTO {
    @NotBlank(message = "Thiếu loại sản phẩm")
    @Pattern(regexp = "tour|car|flight", message = "Loại sản phẩm phải là tour, car hoặc flight")
    private String itemType;

    @NotNull(message = "Thiếu thông tin đặt chỗ")
    private JsonNode booking;

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public JsonNode getBooking() {
        return booking;
    }

    public void setBooking(JsonNode booking) {
        this.booking = booking;
    }
}
