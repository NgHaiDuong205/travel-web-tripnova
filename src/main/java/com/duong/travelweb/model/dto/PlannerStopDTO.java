package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/** Một điểm dừng trong lịch trình AI: khách sạn, điểm tham quan hoặc bữa ăn (kèm toạ độ để vẽ bản đồ). */
public class PlannerStopDTO {
    private String kind;
    private String entityType;
    private UUID entityId;
    private String name;
    private String title;
    private String category;
    private Double latitude;
    private Double longitude;
    private String address;
    private String coverImageUrl;
    private String openingHours;
    private LocalTime startTime;
    private LocalTime endTime;
    private String notes;
    private BigDecimal estimatedCost;
    private Integer travelMinutes;
    private Double distanceKm;
    /** Cách tới điểm này từ điểm trước: walk | drive (null khi thiếu toạ độ). */
    private String travelMode;
    private Boolean mayBeClosed;
    private Integer starRating;
    private BigDecimal pricePerNight;
    private Double rating;
    private Integer reviewCount;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public Integer getTravelMinutes() {
        return travelMinutes;
    }

    public void setTravelMinutes(Integer travelMinutes) {
        this.travelMinutes = travelMinutes;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Boolean getMayBeClosed() {
        return mayBeClosed;
    }

    public void setMayBeClosed(Boolean mayBeClosed) {
        this.mayBeClosed = mayBeClosed;
    }

    public Integer getStarRating() {
        return starRating;
    }

    public void setStarRating(Integer starRating) {
        this.starRating = starRating;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(String travelMode) {
        this.travelMode = travelMode;
    }
}
