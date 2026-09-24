package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body tạo/sửa tiện ích (admin). */
public class AmenityRequestDTO {
    @NotBlank(message = "Tên tiện ích không được trống")
    @Size(max = 255, message = "Tên tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Thiếu nhóm tiện ích")
    @Pattern(regexp = "^[a-z][a-z_]{0,49}$", message = "Nhóm chỉ gồm chữ thường a-z và dấu _ (vd: room, food)")
    private String category;

    private String iconUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
