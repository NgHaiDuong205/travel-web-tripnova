package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** Body tạo / sửa xe (admin). */
public class CarRequestDTO {
    private UUID destinationId;

    @NotBlank(message = "Tên xe không được để trống")
    @Size(max = 150, message = "Tên xe tối đa 150 ký tự")
    private String name;

    @Size(max = 80, message = "Hãng xe tối đa 80 ký tự")
    private String brand;

    @Size(max = 80, message = "Dòng xe tối đa 80 ký tự")
    private String model;

    @Size(max = 20, message = "Biển số tối đa 20 ký tự")
    private String licensePlate;

    @NotBlank(message = "Thiếu loại xe")
    private String carType;

    @NotNull(message = "Thiếu số chỗ")
    @Min(value = 1, message = "Số chỗ phải từ 1 đến 60")
    @Max(value = 60, message = "Số chỗ phải từ 1 đến 60")
    private Integer seats;

    @Size(max = 20, message = "Hộp số tối đa 20 ký tự")
    private String transmission;

    @Size(max = 20, message = "Nhiên liệu tối đa 20 ký tự")
    private String fuelType;

    @NotNull(message = "Thiếu giá thuê / ngày")
    @DecimalMin(value = "0.01", message = "Giá thuê phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá thuê không hợp lệ")
    private BigDecimal pricePerDay;

    private Boolean withDriver;

    @Size(max = 1000, message = "Nơi nhận xe tối đa 1000 ký tự")
    private String pickupLocation;

    private String description;

    @Size(max = 1000, message = "URL ảnh tối đa 1000 ký tự")
    private String coverImageUrl;

    private Boolean isActive;

    public UUID getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(UUID destinationId) {
        this.destinationId = destinationId;
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

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
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

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

