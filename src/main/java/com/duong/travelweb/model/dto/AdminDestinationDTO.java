package com.duong.travelweb.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Điểm đến cho màn admin. */
public class AdminDestinationDTO {
    private UUID id;
    private UUID countryId;
    private String countryName;
    private String name;
    private String description;
    private String coverImageUrl;
    private Double latitude;
    private Double longitude;
    private Boolean isPopular;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private long hotelCount;
    private long landmarkCount;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCountryId() {
        return countryId;
    }

    public void setCountryId(UUID countryId) {
        this.countryId = countryId;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
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

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
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

    public Boolean getIsPopular() {
        return isPopular;
    }

    public void setIsPopular(Boolean isPopular) {
        this.isPopular = isPopular;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getHotelCount() {
        return hotelCount;
    }

    public void setHotelCount(long hotelCount) {
        this.hotelCount = hotelCount;
    }

    public long getLandmarkCount() {
        return landmarkCount;
    }

    public void setLandmarkCount(long landmarkCount) {
        this.landmarkCount = landmarkCount;
    }
}
