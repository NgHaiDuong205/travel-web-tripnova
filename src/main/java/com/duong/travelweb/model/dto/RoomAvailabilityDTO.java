package com.duong.travelweb.model.dto;

import java.time.LocalDate;

/** 1 ngày không trống của phòng (booked / blocked). Ngày không có trong danh sách là còn trống. */
public class RoomAvailabilityDTO {
    private LocalDate date;
    private String status;
    private String reason;

    public RoomAvailabilityDTO() {
    }

    public RoomAvailabilityDTO(LocalDate date, String status, String reason) {
        this.date = date;
        this.status = status;
        this.reason = reason;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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
