package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Danh sách khách sạn gán cho quản lý. */
public class HotelAssignmentRequestDTO {
    @NotNull(message = "Danh sách khách sạn không được để trống")
    private List<UUID> hotelIds;

    public List<UUID> getHotelIds() {
        return hotelIds;
    }

    public void setHotelIds(List<UUID> hotelIds) {
        this.hotelIds = hotelIds;
    }
}
