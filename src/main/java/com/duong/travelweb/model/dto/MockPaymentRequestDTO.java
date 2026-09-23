package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotNull;

/** Giả lập kết quả trả về từ cổng thanh toán (chỉ bật khi app.payment.mock-enabled=true). */
public class MockPaymentRequestDTO {
    @NotNull(message = "Thiếu kết quả thanh toán")
    private Boolean success;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
