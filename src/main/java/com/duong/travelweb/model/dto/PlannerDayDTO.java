package com.duong.travelweb.model.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Một ngày trong lịch trình AI. */
public class PlannerDayDTO {
    private int dayNumber;
    private LocalDate date;
    private String theme;
    private Double distanceKm;
    /** osrm = thời gian theo đường đi thật; estimate = ước lượng đường chim bay. */
    private String routing;
    private List<PlannerStopDTO> items = new ArrayList<>();

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public List<PlannerStopDTO> getItems() {
        return items;
    }

    public void setItems(List<PlannerStopDTO> items) {
        this.items = items;
    }

    public String getRouting() {
        return routing;
    }

    public void setRouting(String routing) {
        this.routing = routing;
    }
}
