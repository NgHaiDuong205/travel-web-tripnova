package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UserRolesRequestDTO {
    @NotEmpty(message = "Phải chọn ít nhất 1 role")
    private List<String> roles;

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
