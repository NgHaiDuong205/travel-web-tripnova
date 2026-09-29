package com.duong.travelweb.model.dto;

import java.util.UUID;

/** Khách sạn thuộc tour. */
public class TourHotelDTO {
    private UUID hotelId;

    private String name;

    private String destinationName;

    private Integer starRating;

    private String imageUrl;

    private String description;

    private Integer checkInDay;

    private Integer nights;

    public UUID getHotelId() {
        return hotelId;
    }

    public void setHotelId(UUID hotelId) {
        this.hotelId = hotelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public Integer getStarRating() {
        return starRating;
    }

    public void setStarRating(Integer starRating) {
        this.starRating = starRating;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCheckInDay() {
        return checkInDay;
    }

    public void setCheckInDay(Integer checkInDay) {
        this.checkInDay = checkInDay;
    }

    public Integer getNights() {
        return nights;
    }

    public void setNights(Integer nights) {
        this.nights = nights;
    }
}

