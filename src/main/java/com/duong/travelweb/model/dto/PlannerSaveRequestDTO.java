package com.duong.travelweb.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Body lưu lịch trình AI vào /api/me/itineraries (items được kiểm tra như thêm hoạt động thường). */
public class PlannerSaveRequestDTO {
    @NotBlank(message = "Vui lòng nhập tên lịch trình")
    @Size(max = 255, message = "Tên lịch trình tối đa 255 ký tự")
    private String title;

    @NotNull(message = "Vui lòng chọn điểm đến")
    private UUID destinationId;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu")
    private LocalDate startDate;

    @NotNull(message = "Vui lòng chọn ngày kết thúc")
    private LocalDate endDate;

    @Min(value = 1, message = "Số người tối thiểu là 1")
    @Max(value = 50, message = "Số người tối đa là 50")
    private Integer partySize;

    @DecimalMin(value = "0", message = "Ngân sách không được âm")
    private BigDecimal totalBudget;

    @Size(max = 2000, message = "Prompt tối đa 2000 ký tự")
    private String prompt;

    @Size(max = 100, message = "modelVersion tối đa 100 ký tự")
    private String modelVersion;

    @NotEmpty(message = "Lịch trình chưa có hoạt động")
    @Size(max = 120, message = "Tối đa 120 hoạt động")
    private List<@Valid ItineraryItemRequestDTO> items = new ArrayList<>();

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

    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public List<ItineraryItemRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<ItineraryItemRequestDTO> items) {
        this.items = items;
    }
}
