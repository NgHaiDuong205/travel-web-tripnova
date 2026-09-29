package com.duong.travelweb.model.dto;

import java.time.LocalDateTime;

/** Khoảng thời gian xe đã được giữ / thuê. */
public class CarBusyRangeDTO {
    private LocalDateTime from;

    private LocalDateTime to;

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }
}

