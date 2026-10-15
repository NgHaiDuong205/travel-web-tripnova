package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Ghi một tương tác của user (xem trang chi tiết...) làm tín hiệu cho gợi ý. */
public class InteractionRequestDTO {
    @NotBlank
    private String entityType;
    @NotNull
    private UUID entityId;
    /** view (mặc định) | click | share */
    private String action;

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
