package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "hotel_amenities")
public class HotelAmenityEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "hotel_id")
    private HotelEntity hotel;

    @ManyToOne
    @JoinColumn(name = "amenity_id")
    private AmenityEntity amenity;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public HotelEntity getHotel() {
        return hotel;
    }

    public void setHotel(HotelEntity hotel) {
        this.hotel = hotel;
    }

    public AmenityEntity getAmenity() {
        return amenity;
    }

    public void setAmenity(AmenityEntity amenity) {
        this.amenity = amenity;
    }
}
