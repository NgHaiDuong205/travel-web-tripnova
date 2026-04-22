package com.duong.travelweb.repository.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "cities")
public class CityEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name",nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
    @ManyToOne
    @JoinColumn(name = "country_id")
    private CountryEntity country;
    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<HotelEntity> items;

    public List<HotelEntity> getItems() {
        return items;
    }

    public void setItems(List<HotelEntity> items) {
        this.items = items;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CountryEntity getCountry() {
        return country;
    }

    public void setCountry(CountryEntity country) {
        this.country = country;
    }
}
