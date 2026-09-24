package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Body khoá/mở ngày của 1 phòng (admin), khoảng [from, to] tính cả 2 đầu. */
public class RoomAvailabilityRequestDTO {
    @NotNull(message = "Thiếu ngày bắt đầu")
    private LocalDate from;

    @NotNull(message = "Thiếu ngày kết thúc")
    private LocalDate to;

    @NotBlank(message = "Thiếu trạng thái")
    @Pattern(regexp = "^(available|blocked)$", message = "Trạng thái phải là available hoặc blocked")
    private String status;

    @Size(max = 200, message = "Lý do tối đa 200 ký tự")
    private String reason;

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

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
