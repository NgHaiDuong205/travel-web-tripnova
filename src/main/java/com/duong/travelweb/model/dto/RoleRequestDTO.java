package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body tạo / sửa role. */
public class RoleRequestDTO {
    @NotBlank(message = "Tên role không được để trống")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "Tên role chỉ gồm chữ in hoa, số, dấu _ (2-50 ký tự)")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
