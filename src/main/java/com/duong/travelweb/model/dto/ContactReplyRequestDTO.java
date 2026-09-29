package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body admin trả lời tin nhắn liên hệ. */
public class ContactReplyRequestDTO {
    @NotBlank(message = "Vui lòng nhập nội dung trả lời")
    @Size(max = 5000, message = "Nội dung trả lời tối đa 5000 ký tự")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
