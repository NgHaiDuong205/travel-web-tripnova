package com.duong.travelweb.model.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/** Body hoàn tiền của admin. amount chỉ dùng cho hoàn tiền theo booking (null = hoàn toàn bộ phần còn lại). */
public class RefundRequestDTO {
    @DecimalMin(value = "0.01", message = "Số tiền hoàn phải lớn hơn 0")
    private BigDecimal amount;

    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
