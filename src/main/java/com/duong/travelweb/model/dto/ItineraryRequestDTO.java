package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Body tạo / sửa lịch trình (PUT ghi đè toàn bộ các trường). */
public class ItineraryRequestDTO {
    @NotBlank(message = "Vui lòng nhập tên lịch trình")
    @Size(max = 255, message = "Tên lịch trình tối đa 255 ký tự")
    private String title;

    private UUID destinationId;
    private LocalDate startDate;
    private LocalDate endDate;

    @DecimalMin(value = "0", message = "Ngân sách không được âm")
    private BigDecimal totalBudget;

    @Min(value = 1, message = "Số người tối thiểu là 1")
    @Max(value = 50, message = "Số người tối đa là 50")
    private Integer partySize;

    @Pattern(regexp = "^(draft|saved|booked)?$", message = "Trạng thái không hợp lệ")
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(UUID destinationId) {
        this.destinationId = destinationId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
