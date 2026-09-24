package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body tạo/sửa châu lục (admin). */
public class ContinentRequestDTO {
    @NotBlank(message = "Thiếu mã châu lục")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Mã châu lục gồm 2 chữ cái")
    private String code;

    @NotBlank(message = "Tên châu lục không được trống")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
