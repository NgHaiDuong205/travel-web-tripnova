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
    private List<String> typeCode;
    private HotelSearchBuilder(Builder builder){
        this.id = builder.id;
        this.destinationId = builder.destinationId;
        this.managedById = builder.managedById;
        this.name = builder.name;
        this.starRating = builder.starRating;
        this.typeCode = builder.typeCode;
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

    public List<String> getTypeCode() {
        return typeCode;
    }

    public static class Builder{
        private UUID id;
        private UUID destinationId;
        private UUID managedById;
        private String name;
        private Integer starRating;
        private List<String> typeCode = new ArrayList<>();
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
        public Builder typeCode(List<String> typeCode){
            this.typeCode = typeCode;
            return this;
        }
        public HotelSearchBuilder build(){
            return new HotelSearchBuilder(this);
        }
    }
}
