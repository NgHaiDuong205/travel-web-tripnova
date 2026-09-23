package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public class FavoriteRequestDTO {
    @NotBlank(message = "Thiếu loại mục yêu thích")
    @Pattern(regexp = "hotel|tour|car|flight", message = "Loại mục yêu thích không hợp lệ")
    private String itemType;

    @NotNull(message = "Thiếu mã mục yêu thích")
    private UUID itemId;

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }
}
