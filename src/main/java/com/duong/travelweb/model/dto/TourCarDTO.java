package com.duong.travelweb.model.dto;

import java.util.UUID;

/** Xe thuộc tour. */
public class TourCarDTO {
    private UUID carId;

    private String name;

    private String carType;

    private Integer seats;

    private Boolean withDriver;

    private String imageUrl;

    private Integer usageDay;

    public UUID getCarId() {
        return carId;
    }

    public void setCarId(UUID carId) {
        this.carId = carId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public Boolean getWithDriver() {
        return withDriver;
    }

    public void setWithDriver(Boolean withDriver) {
        this.withDriver = withDriver;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getUsageDay() {
        return usageDay;
    }

    public void setUsageDay(Integer usageDay) {
        this.usageDay = usageDay;
    }
}

