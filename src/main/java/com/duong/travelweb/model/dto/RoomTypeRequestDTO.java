package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Body tạo/sửa hạng phòng (admin). */
public class RoomTypeRequestDTO {
    @NotBlank(message = "Tên hạng phòng không được trống")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String name;

    private String description;

    @Min(value = 1, message = "Số khách tối thiểu là 1")
    @Max(value = 20, message = "Số khách tối đa là 20")
    private Integer maxOccupancy;

    @Size(max = 60, message = "Loại giường tối đa 60 ký tự")
    private String bedType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Diện tích phải lớn hơn 0")
    @DecimalMax(value = "9999.99", message = "Diện tích quá lớn")
    private Double areaSqM;

    @NotNull(message = "Thiếu giá mỗi đêm")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    @DecimalMax(value = "9999999999.99", message = "Giá quá lớn")
    private Double pricePerNight;

    private String coverImageUrl;

    private List<UUID> amenityIds;

    private Boolean isActive;

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

    public Integer getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(Integer maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }

    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public Double getAreaSqM() {
        return areaSqM;
    }

    public void setAreaSqM(Double areaSqM) {
        this.areaSqM = areaSqM;
    }

    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public List<UUID> getAmenityIds() {
        return amenityIds;
    }

    public void setAmenityIds(List<UUID> amenityIds) {
        this.amenityIds = amenityIds;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
