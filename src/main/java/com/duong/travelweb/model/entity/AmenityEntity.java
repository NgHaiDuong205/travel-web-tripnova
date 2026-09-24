package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "amenities")
public class AmenityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "category")
    private String category;

    // Không cascade: room_type_amenities đã ON DELETE CASCADE ở DB
    @OneToMany(mappedBy = "amenity", fetch = FetchType.LAZY)
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

    public List<RoomTypeAmenityEntity> getRoomTypeAmenities() {
        return roomTypeAmenities;
    }

    public void setRoomTypeAmenities(List<RoomTypeAmenityEntity> roomTypeAmenities) {
        this.roomTypeAmenities = roomTypeAmenities;
    }
}
