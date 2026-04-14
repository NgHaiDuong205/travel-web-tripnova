package com.duong.travelweb.builder;

import java.util.ArrayList;
import java.util.List;

public class HotelSearchBuider {
    private Integer id;
    private String countryId;
    private Integer cityId;
    private String name;
    private Double rating;
    private List<String> typeCode;
    private HotelSearchBuider(Buider buider){
        this.id = buider.id;
        this.countryId = buider.countryId;
        this.cityId = buider.cityId;
        this.name = buider.name;
        this.rating = buider.rating;
        this.typeCode = buider.typeCode;
    }
    public Integer getId() {
        return id;
    }

    public String getCountryId() {
        return countryId;
    }

    public Integer getCityId() {
        return cityId;
    }

    public String getName() {
        return name;
    }

    public Double getRating() {
        return rating;
    }

    public List<String> getTypeCode() {
        return typeCode;
    }

    public static class Buider{
        private Integer id;
        private String countryId;
        private Integer cityId;
        private String name;
        private Double rating;
        private List<String> typeCode = new ArrayList<>();
        public Buider id(Integer id){
            this.id = id;
            return this;
        }
        public Buider countryId(String countryId){
            this.countryId = countryId;
            return this;
        }
        public Buider cityId(Integer cityId){
            this.cityId = cityId;
            return this;
        }
        public Buider name(String name){
            this.name = name;
            return this;
        }
        public Buider rating(Double rating){
            this.rating = rating;
            return this;
        }
        public Buider typeCode(List<String> typeCode){
            this.typeCode = typeCode;
            return this;
        }
        public HotelSearchBuider build(){
            return new HotelSearchBuider(this);
        }
    }
}
