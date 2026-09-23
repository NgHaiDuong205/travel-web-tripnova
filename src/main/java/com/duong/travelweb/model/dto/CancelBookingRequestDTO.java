package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Size;

public class CancelBookingRequestDTO {
    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
