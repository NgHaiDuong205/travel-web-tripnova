package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Body tạo/sửa quốc gia (admin). */
public class CountryRequestDTO {
    private UUID id;

    @NotBlank(message = "Tên quốc gia không được trống")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Thiếu mã quốc gia")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Mã quốc gia gồm 3 chữ cái (ISO alpha-3)")
    private String countryCode;

    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug chỉ gồm chữ thường, số và dấu -")
    @Size(max = 150, message = "Slug tối đa 150 ký tự")
    private String slug;

    @Size(max = 255, message = "URL ảnh tối đa 255 ký tự")
    private String imageUrl;
    private String description;

    @NotNull(message = "Thiếu châu lục")
    private UUID continentId;

    @DecimalMin(value = "-90", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90", message = "Vĩ độ không hợp lệ")
    private Double latitude;

    @DecimalMin(value = "-180", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180", message = "Kinh độ không hợp lệ")
    private Double longitude;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getContinentId() {
        return continentId;
    }

    public void setContinentId(UUID continentId) {
        this.continentId = continentId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
