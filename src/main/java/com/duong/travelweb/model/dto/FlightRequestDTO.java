package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Body tạo / sửa chuyến bay (admin). */
public class FlightRequestDTO {
    @NotBlank(message = "Thiếu số hiệu chuyến bay")
    @Size(max = 20, message = "Số hiệu tối đa 20 ký tự")
    private String flightNumber;

    @NotBlank(message = "Thiếu hãng bay")
    @Size(max = 100, message = "Tên hãng tối đa 100 ký tự")
    private String airline;

    @Size(max = 1000, message = "URL logo tối đa 1000 ký tự")
    private String airlineLogoUrl;

    @NotBlank(message = "Thiếu sân bay đi")
    @Size(max = 10, message = "Mã sân bay tối đa 10 ký tự")
    private String departureAirportCode;

    @NotBlank(message = "Thiếu sân bay đến")
    @Size(max = 10, message = "Mã sân bay tối đa 10 ký tự")
    private String arrivalAirportCode;

    @Size(max = 100, message = "Tên thành phố tối đa 100 ký tự")
    private String departureCity;

    @Size(max = 100, message = "Tên thành phố tối đa 100 ký tự")
    private String arrivalCity;

    @NotNull(message = "Thiếu giờ khởi hành")
    private LocalDateTime departureTime;

    @NotNull(message = "Thiếu giờ đến")
    private LocalDateTime arrivalTime;

    @Size(max = 50, message = "Loại máy bay tối đa 50 ký tự")
    private String aircraftType;

    @NotNull(message = "Thiếu giá cơ bản")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá không hợp lệ")
    private BigDecimal basePrice;

    private String baggagePolicy;

    private Boolean isActive;

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getAirlineLogoUrl() {
        return airlineLogoUrl;
    }

    public void setAirlineLogoUrl(String airlineLogoUrl) {
        this.airlineLogoUrl = airlineLogoUrl;
    }

    public String getDepartureAirportCode() {
        return departureAirportCode;
    }

    public void setDepartureAirportCode(String departureAirportCode) {
        this.departureAirportCode = departureAirportCode;
    }

    public String getArrivalAirportCode() {
        return arrivalAirportCode;
    }

    public void setArrivalAirportCode(String arrivalAirportCode) {
        this.arrivalAirportCode = arrivalAirportCode;
    }

    public String getDepartureCity() {
        return departureCity;
    }

    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
    }

    public String getArrivalCity() {
        return arrivalCity;
    }

    public void setArrivalCity(String arrivalCity) {
        this.arrivalCity = arrivalCity;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getAircraftType() {
        return aircraftType;
    }

    public void setAircraftType(String aircraftType) {
        this.aircraftType = aircraftType;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getBaggagePolicy() {
        return baggagePolicy;
    }

    public void setBaggagePolicy(String baggagePolicy) {
        this.baggagePolicy = baggagePolicy;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

