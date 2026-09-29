package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Body thanh toán giỏ hàng. itemIds null/rỗng = thanh toán toàn bộ giỏ. */
public class CartCheckoutRequestDTO {
    @NotBlank(message = "Thiếu phương thức thanh toán")
    @Pattern(regexp = "credit_card|debit_card|bank_transfer|e_wallet|momo|zalopay|vnpay|cash",
            message = "Phương thức thanh toán không hợp lệ")
    private String paymentMethod;

    private List<UUID> itemIds = new ArrayList<>();

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<UUID> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<UUID> itemIds) {
        this.itemIds = itemIds;
    }
}
