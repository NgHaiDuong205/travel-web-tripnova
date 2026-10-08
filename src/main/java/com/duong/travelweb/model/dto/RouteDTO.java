package com.duong.travelweb.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Đường đi qua các điểm. source = osrm (đường bộ thật) | none (không lấy được → FE nối thẳng các điểm).
 * durationMinutes là thời gian đi ô tô khi đường thông thoáng (chưa tính kẹt xe).
 */
public class RouteDTO {
    private String source;
    private Double distanceKm;
    private Integer durationMinutes;
    private List<double[]> geometry = new ArrayList<>();

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public List<double[]> getGeometry() {
        return geometry;
    }

    public void setGeometry(List<double[]> geometry) {
        this.geometry = geometry;
    }
}
