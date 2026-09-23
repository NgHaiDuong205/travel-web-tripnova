package com.duong.travelweb.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class FavoriteDTO {
    private UUID id;
    private String itemType;
    private UUID itemId;
    private LocalDateTime createdAt;
    /** Chi tiết khách sạn khi itemType = hotel (null nếu khách sạn không còn). */
    private HotelDTO hotel;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public HotelDTO getHotel() {
        return hotel;
    }

    public void setHotel(HotelDTO hotel) {
        this.hotel = hotel;
    }
}
