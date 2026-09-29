package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

/** Thanh toán giỏ chuyến đi theo từng loại: mọi dòng (hoặc itemIds) của itemType -> 1 order + 1 payment. */
public class TripCartCheckoutRequestDTO {
    @NotBlank(message = "Thiếu loại sản phẩm")
    @Pattern(regexp = "tour|car|flight", message = "Loại sản phẩm phải là tour, car hoặc flight")
    private String itemType;

    private List<UUID> itemIds;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    @Pattern(regexp = "credit_card|debit_card|bank_transfer|e_wallet|momo|zalopay|vnpay|cash",
            message = "Phương thức thanh toán không hợp lệ")
    private String paymentMethod;

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public List<UUID> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<UUID> itemIds) {
        this.itemIds = itemIds;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
