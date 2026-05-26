package com.duong.travelweb.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HotelSearchBuilder {
    private UUID id;
    private UUID destinationId;
    private UUID managedById;
    private String name;
    private Integer starRating;
    private Integer minStarRating;
    private Integer maxStarRating;
    private Integer totalRooms;
    private String countryName;
    private String destinationName;
    private List<String> amenities = new ArrayList<>();
    private List<String> typeCode = new ArrayList<>();
    private Integer page;
    private Integer limit;

    private HotelSearchBuilder(Builder builder){
        this.id = builder.id;
        this.destinationId = builder.destinationId;
        this.managedById = builder.managedById;
        this.name = builder.name;
        this.starRating = builder.starRating;
        this.minStarRating = builder.minStarRating;
        this.maxStarRating = builder.maxStarRating;
        this.totalRooms = builder.totalRooms;
        this.countryName = builder.countryName;
        this.destinationName = builder.destinationName;
        this.amenities = builder.amenities;
        this.typeCode = builder.typeCode;
        this.page = builder.page;
        this.limit = builder.limit;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public UUID getManagedById() {
        return managedById;
    }

    public String getName() {
        return name;
    }

    public Integer getStarRating() {
        return starRating;
    }

    public Integer getMinStarRating() {
        return minStarRating;
    }

    public Integer getMaxStarRating() {
        return maxStarRating;
    }

    public Integer getTotalRooms() {
        return totalRooms;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public List<String> getTypeCode() {
        return typeCode;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLimit() {
        return limit;
    }

    public static class Builder{
        private UUID id;
        private UUID destinationId;
        private UUID managedById;
        private String name;
        private Integer starRating;
        private Integer minStarRating;
        private Integer maxStarRating;
        private Integer totalRooms;
        private String countryName;
        private String destinationName;
        private List<String> amenities = new ArrayList<>();
        private List<String> typeCode = new ArrayList<>();
        private Integer page;
        private Integer limit;

        public Builder id(UUID id){
            this.id = id;
            return this;
        }
        public Builder destinationId(UUID destinationId){
            this.destinationId = destinationId;
            return this;
        }
        public Builder managedById(UUID managedById){
            this.managedById = managedById;
            return this;
        }
        public Builder name(String name){
            this.name = name;
            return this;
        }
        public Builder starRating(Integer starRating){
            this.starRating = starRating;
            return this;
        }
        public Builder minStarRating(Integer minStarRating){
            this.minStarRating = minStarRating;
            return this;
        }
        public Builder maxStarRating(Integer maxStarRating){
            this.maxStarRating = maxStarRating;
            return this;
        }
        public Builder totalRooms(Integer totalRooms){
            this.totalRooms = totalRooms;
            return this;
        }
        public Builder countryName(String countryName){
            this.countryName = countryName;
            return this;
        }
        public Builder destinationName(String destinationName){
            this.destinationName = destinationName;
            return this;
        }
        public Builder amenities(List<String> amenities){
            if (amenities != null) {
                this.amenities = amenities;
            }
            return this;
        }
        public Builder typeCode(List<String> typeCode){
            if (typeCode != null) {
                this.typeCode = typeCode;
            }
            return this;
        }
        public Builder page(Integer page){
            this.page = page;
            return this;
        }
        public Builder limit(Integer limit){
            this.limit = limit;
            return this;
        }
        public HotelSearchBuilder build(){
            return new HotelSearchBuilder(this);
        }
    }
}
