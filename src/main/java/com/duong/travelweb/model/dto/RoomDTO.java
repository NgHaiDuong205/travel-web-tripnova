package com.duong.travelweb.model.dto;

import java.util.UUID;

public class RoomDTO {
    private UUID id;
    private String roomNumber;
    private Integer floor;
    private String status;
    private UUID roomTypeId;
    private String roomTypeName;
    private Double pricePerNight;
    private Integer maxOccupancy;
    private String bedType;
    private Double areaSqM;
    private java.util.List<String> amenities;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public java.util.List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(java.util.List<String> amenities) {
        this.amenities = amenities;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(UUID roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public Integer getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(Integer maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }

    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public Double getAreaSqM() {
        return areaSqM;
    }

    public void setAreaSqM(Double areaSqM) {
        this.areaSqM = areaSqM;
    }
}
