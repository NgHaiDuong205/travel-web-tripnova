package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body chung cho các API admin đổi trạng thái. */
public class StatusUpdateRequestDTO {
    @NotBlank(message = "Thiếu trạng thái mới")
    private String status;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String reason;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
