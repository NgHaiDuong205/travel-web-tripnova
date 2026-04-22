package com.duong.travelweb.repository.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "hotels")
public class HotelEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name" , nullable = false)
    private String name;

    @Column(name = "address",nullable = false)
    private String address;

    @Column(name = "rating",nullable = false)
    private Double rating;

    @Column(name = "amenities",nullable = false)
    private String amenities;

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createAt;

    @Column(name = "description")
    private String description;

    @Column(name = "repair_at")
    private LocalDateTime repairAt;
    @ManyToOne
    @JoinColumn(name = "city_id")
    private CityEntity city;

    public CityEntity getCity() {
        return city;
    }

    public void setCity(CityEntity city) {
        this.city = city;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
