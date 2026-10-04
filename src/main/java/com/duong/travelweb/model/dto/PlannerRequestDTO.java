package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Body tạo lịch trình bằng AI Planner (chưa lưu). */
public class PlannerRequestDTO {
    @NotNull(message = "Vui lòng chọn điểm đến")
    private UUID destinationId;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu")
    private LocalDate startDate;

    @NotNull(message = "Vui lòng chọn số ngày")
    @Min(value = 1, message = "Tối thiểu 1 ngày")
    @Max(value = 7, message = "Tối đa 7 ngày")
    private Integer days;

    @NotNull(message = "Vui lòng nhập số người")
    @Min(value = 1, message = "Số người tối thiểu là 1")
    @Max(value = 20, message = "Số người tối đa là 20")
    private Integer partySize;

    @Pattern(regexp = "^(budget|mid|luxury)?$", message = "Mức ngân sách không hợp lệ")
    private String budget;

    @Pattern(regexp = "^(relaxed|balanced|packed)?$", message = "Nhịp độ không hợp lệ")
    private String pace;

    @Size(max = 7, message = "Tối đa 7 sở thích")
    private List<@Pattern(regexp = "^(culture|nature|beach|food|shopping|entertainment|wellness)$", message = "Sở thích không hợp lệ") String> interests = new ArrayList<>();

    @Size(max = 300, message = "Ghi chú tối đa 300 ký tự")
    private String note;

    @Pattern(regexp = "^(vi|en)?$", message = "Ngôn ngữ không hợp lệ")
    private String locale;

    @Min(value = 0, message = "variant không hợp lệ")
    @Max(value = 1000, message = "variant không hợp lệ")
    private Integer variant;

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

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getPace() {
        return pace;
    }

    public void setPace(String pace) {
        this.pace = pace;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Integer getVariant() {
        return variant;
    }

    public void setVariant(Integer variant) {
        this.variant = variant;
    }
}
