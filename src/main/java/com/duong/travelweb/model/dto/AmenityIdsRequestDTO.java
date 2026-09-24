package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Body gán danh sách tiện ích (admin). */
public class AmenityIdsRequestDTO {
    @NotNull(message = "Thiếu danh sách tiện ích")
    private List<UUID> amenityIds;

    public List<UUID> getAmenityIds() {
        return amenityIds;
    }

    public void setAmenityIds(List<UUID> amenityIds) {
        this.amenityIds = amenityIds;
    }
}
