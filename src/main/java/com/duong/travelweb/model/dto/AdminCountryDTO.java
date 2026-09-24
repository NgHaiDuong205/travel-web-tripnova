package com.duong.travelweb.model.dto;

import java.util.UUID;

/** Quốc gia cho màn admin. */
public class AdminCountryDTO {
    private UUID id;
    private UUID continentId;
    private String continentName;
    private String countryCode;
    private String name;
    private String slug;
    private String imageUrl;
    private String description;
    private Double latitude;
    private Double longitude;
    private long destinationCount;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getContinentId() {
        return continentId;
    }

    public void setContinentId(UUID continentId) {
        this.continentId = continentId;
    }

    public String getContinentName() {
        return continentName;
    }

    public void setContinentName(String continentName) {
        this.continentName = continentName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public long getDestinationCount() {
        return destinationCount;
    }

    public void setDestinationCount(long destinationCount) {
        this.destinationCount = destinationCount;
    }
}
