package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/** Body thêm hoạt động vào lịch trình. */
public class ItineraryItemRequestDTO {
    @NotNull(message = "Thiếu ngày thứ mấy")
    @Min(value = 1, message = "Ngày phải từ 1 trở lên")
    private Integer dayNumber;

    private LocalTime startTime;
    private LocalTime endTime;

    @Pattern(regexp = "^(hotel|landmark|destination|restaurant|activity|transport|other)?$", message = "Loại hoạt động không hợp lệ")
    private String entityType;

    private UUID entityId;

    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    private String notes;

    @DecimalMin(value = "0", message = "Chi phí không được âm")
    private BigDecimal estimatedCost;

    @Min(value = 0, message = "Thứ tự không được âm")
    private Integer sortOrder;

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
