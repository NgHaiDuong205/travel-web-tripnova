package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactRequestDTO {
    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String phone;

    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String subject;

    @NotBlank(message = "Vui lòng nhập nội dung")
    @Size(max = 5000, message = "Nội dung tối đa 5000 ký tự")
    private String message;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
