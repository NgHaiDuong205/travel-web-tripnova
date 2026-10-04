package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Một xe có thể thuê cho lịch trình (giá theo số ngày thuê). */
public class TripCarOptionDTO {
    private UUID carId;
    private String name;
    private String brand;
    private String model;
    private String carType;
    private Integer seats;
    private String transmission;
    private String fuelType;
    private Boolean withDriver;
    private String pickupLocation;
    private String coverImageUrl;
    private BigDecimal pricePerDay;
    private int rentalDays;
    private BigDecimal subtotal;
    private boolean available;
    private boolean fitsParty;
    private LocalDateTime freeCancellationUntil;

    public UUID getCarId() {
        return carId;
    }

    public void setCarId(UUID carId) {
        this.carId = carId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public Boolean getWithDriver() {
        return withDriver;
    }

    public void setWithDriver(Boolean withDriver) {
        this.withDriver = withDriver;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public void setRentalDays(int rentalDays) {
        this.rentalDays = rentalDays;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isFitsParty() {
        return fitsParty;
    }

    public void setFitsParty(boolean fitsParty) {
        this.fitsParty = fitsParty;
    }

    public LocalDateTime getFreeCancellationUntil() {
        return freeCancellationUntil;
    }

    public void setFreeCancellationUntil(LocalDateTime freeCancellationUntil) {
        this.freeCancellationUntil = freeCancellationUntil;
    }
}

