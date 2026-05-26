package com.duong.travelweb.model.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RoomTypeAmenityId implements Serializable {
    private UUID roomType;
    private UUID amenity;

    public RoomTypeAmenityId() {}

    public RoomTypeAmenityId(UUID roomType, UUID amenity) {
        this.roomType = roomType;
        this.amenity = amenity;
    }

    public UUID getRoomType() {
        return roomType;
    }

    public void setRoomType(UUID roomType) {
        this.roomType = roomType;
    }

    public UUID getAmenity() {
        return amenity;
    }

    public void setAmenity(UUID amenity) {
        this.amenity = amenity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomTypeAmenityId that = (RoomTypeAmenityId) o;
        return Objects.equals(roomType, that.roomType) && Objects.equals(amenity, that.amenity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomType, amenity);
    }
}
