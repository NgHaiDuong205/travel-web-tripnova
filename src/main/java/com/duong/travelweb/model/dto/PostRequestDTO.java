package com.duong.travelweb.model.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Yêu cầu tạo hoặc cập nhật bài viết / đánh giá. */
public class PostRequestDTO {
    @NotBlank(message = "Thiếu loại đối tượng (hotel, landmark, destination)")
    private String entityType;

    @NotNull(message = "Thiếu entityId")
    private UUID entityId;

    @Size(max = 300, message = "Tiêu đề tối đa 300 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 5000, message = "Nội dung tối đa 5000 ký tự")
    private String content;

    @Min(value = 1, message = "Điểm đánh giá từ 1 đến 5")
    @Max(value = 5, message = "Điểm đánh giá từ 1 đến 5")
    private Integer rating;

    @Size(max = 10, message = "Tối đa 10 ảnh")
    private List<@NotBlank(message = "URL ảnh không hợp lệ") @Size(max = 500, message = "URL ảnh tối đa 500 ký tự") String> images;

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
