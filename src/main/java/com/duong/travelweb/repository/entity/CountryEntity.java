package com.duong.travelweb.repository.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name="countries")
public class CountryEntity {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name" , nullable = false)
    private String countryName;

    @Column(name = "flag", nullable = false)
    private String flag;

    @ManyToOne
    @JoinColumn(name = "continent_id")
    private ContinentEntity continent;

    @OneToMany(mappedBy = "country", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CityEntity> items;

    public List<CityEntity> getItems() {
        return items;
    }

    public void setItems(List<CityEntity> items) {
        this.items = items;
    }

    public ContinentEntity getContinent() {
        return continent;
    }

    public void setContinent(ContinentEntity continent) {
        this.continent = continent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

}
