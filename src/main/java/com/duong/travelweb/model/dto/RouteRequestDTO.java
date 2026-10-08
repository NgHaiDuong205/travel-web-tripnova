package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Body vẽ đường đi: các điểm theo thứ tự, mỗi điểm [lat, lng]. */
public class RouteRequestDTO {
    @NotNull(message = "Thiếu danh sách điểm")
    @Size(min = 2, max = 25, message = "Cần từ 2 đến 25 điểm")
    private List<@NotNull(message = "Điểm không hợp lệ") @Size(min = 2, max = 2, message = "Mỗi điểm gồm [lat, lng]") List<@NotNull(message = "Toạ độ không hợp lệ") Double>> points;

    public List<List<Double>> getPoints() {
        return points;
    }

    public void setPoints(List<List<Double>> points) {
        this.points = points;
    }
}
