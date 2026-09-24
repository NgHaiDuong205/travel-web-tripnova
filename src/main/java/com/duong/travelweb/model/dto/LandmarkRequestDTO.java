package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Body tạo/sửa địa danh (admin). */
public class LandmarkRequestDTO {
    @NotNull(message = "Thiếu điểm đến")
    private UUID destinationId;

    @NotBlank(message = "Tên địa danh không được trống")
    @Size(max = 200, message = "Tên tối đa 200 ký tự")
    private String name;

    private String description;

    @Pattern(regexp = "^(beach|temple|museum|park|mountain|market|restaurant|entertainment|historical|other)$", message = "Loại địa danh không hợp lệ")
    private String category;

    private String coverImageUrl;

    @Size(max = 200, message = "Giờ mở cửa tối đa 200 ký tự")
    private String openingHours;

    @DecimalMin(value = "0", message = "Phí vào cửa không được âm")
    private Double entryFee;

    private Boolean isActive;

    public UUID getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(UUID destinationId) {
        this.destinationId = destinationId;
    }

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public Double getEntryFee() {
        return entryFee;
    }

    public void setEntryFee(Double entryFee) {
        this.entryFee = entryFee;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
