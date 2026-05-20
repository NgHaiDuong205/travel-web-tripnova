package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "room_type_amenities")
public class RoomTypeAmenityEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "room_type_id")
    private RoomTypeEntity roomType;

    @ManyToOne
    @JoinColumn(name = "amenity_id")
    private AmenityEntity amenity;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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
