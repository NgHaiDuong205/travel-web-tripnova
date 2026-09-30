package com.duong.travelweb.model.dto;

import java.util.UUID;

/** Khách sạn được gán cho tài khoản quản lý. */
public class ManagedHotelDTO {
    private UUID id;
    private String name;
    private Boolean isActive;
    private String destinationName;

    public ManagedHotelDTO() {
    }

    public ManagedHotelDTO(UUID id, String name, Boolean isActive, String destinationName) {
        this.id = id;
        this.name = name;
        this.isActive = isActive;
        this.destinationName = destinationName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
}
