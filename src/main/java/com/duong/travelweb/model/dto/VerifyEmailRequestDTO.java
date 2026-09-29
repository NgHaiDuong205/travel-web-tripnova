package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequestDTO {
    @NotBlank(message = "Thiếu token xác thực")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
