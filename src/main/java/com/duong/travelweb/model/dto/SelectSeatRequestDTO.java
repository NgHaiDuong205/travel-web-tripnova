package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Đổi ghế của một vé. */
public class SelectSeatRequestDTO {
    @NotNull(message = "Vui lòng chọn ghế")
    private UUID seatId;

    public UUID getSeatId() {
        return seatId;
    }

    public void setSeatId(UUID seatId) {
        this.seatId = seatId;
    }
}

