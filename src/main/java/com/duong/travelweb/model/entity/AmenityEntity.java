package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "amenities")
public class AmenityEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "category")
    private String category;

    @OneToMany(mappedBy = "amenity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<HotelAmenityEntity> hotelAmenities;

    @OneToMany(mappedBy = "amenity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<RoomTypeAmenityEntity> roomTypeAmenities;

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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<HotelAmenityEntity> getHotelAmenities() {
        return hotelAmenities;
    }

    public void setHotelAmenities(List<HotelAmenityEntity> hotelAmenities) {
        this.hotelAmenities = hotelAmenities;
    }

    public List<RoomTypeAmenityEntity> getRoomTypeAmenities() {
        return roomTypeAmenities;
    }

    public void setRoomTypeAmenities(List<RoomTypeAmenityEntity> roomTypeAmenities) {
        this.roomTypeAmenities = roomTypeAmenities;
    }
}
