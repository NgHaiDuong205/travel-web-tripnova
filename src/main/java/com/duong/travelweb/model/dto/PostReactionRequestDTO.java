package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;

/** Yêu cầu phản ứng với bài viết. */
public class PostReactionRequestDTO {
    @NotBlank(message = "Thiếu loại reaction (like, dislike)")
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
