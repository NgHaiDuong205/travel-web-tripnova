package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "room_type_amenities")
@IdClass(RoomTypeAmenityId.class)
public class RoomTypeAmenityEntity {
    @Id
    @ManyToOne
    @JoinColumn(name = "room_type_id")
    private RoomTypeEntity roomType;

    @Id
    @ManyToOne
    @JoinColumn(name = "amenity_id")
    private AmenityEntity amenity;

    public RoomTypeEntity getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomTypeEntity roomType) {
        this.roomType = roomType;
    }

    public AmenityEntity getAmenity() {
        return amenity;
    }

    public void setAmenity(AmenityEntity amenity) {
        this.amenity = amenity;
    }
}
