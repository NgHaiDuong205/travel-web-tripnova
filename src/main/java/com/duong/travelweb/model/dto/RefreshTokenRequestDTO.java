package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequestDTO {
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;


    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
