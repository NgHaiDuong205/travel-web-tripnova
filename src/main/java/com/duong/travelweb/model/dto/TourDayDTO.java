package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Một chặng trong lịch trình tour (vd. Days 1 - 3). */
public class TourDayDTO {
    @NotBlank(message = "Thiếu nhãn ngày")
    @Size(max = 50, message = "Nhãn ngày tối đa 50 ký tự")
    private String label;

    @NotBlank(message = "Thiếu tiêu đề chặng")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

