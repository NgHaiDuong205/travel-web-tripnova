package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotNull;

public class UserStatusRequestDTO {
    @NotNull(message = "Thiếu trạng thái hoạt động")
    private Boolean isActive;

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
