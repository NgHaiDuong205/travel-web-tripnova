package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;

public class DeleteAccountRequestDTO {
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
