package com.duong.travelweb.repository.entity;

import java.time.LocalDateTime;

public class HotelEntity {
    private Integer id;
    private Integer cityId;
    private String name;
    private String address;
    private Double rating;
    private String amenities;
    private LocalDateTime createAt;
    private String description;
    private LocalDateTime repairAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getRepairAt() {
        return repairAt;
    }

    public void setRepairAt(LocalDateTime repairAt) {
        this.repairAt = repairAt;
    }
}
