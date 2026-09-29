package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Số chỗ còn lại của tour theo ngày khởi hành. */
public class TourAvailabilityDTO {
    private UUID tourId;

    private LocalDate departureDate;

    private Integer maxParticipants;

    private Integer booked;

    private Integer spotsLeft;

    private Boolean available;

    private BigDecimal priceAdult;

    private BigDecimal priceChild;

    private String currencyCode;

    public UUID getTourId() {
        return tourId;
    }

    public void setTourId(UUID tourId) {
        this.tourId = tourId;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getBooked() {
        return booked;
    }

    public void setBooked(Integer booked) {
        this.booked = booked;
    }

    public Integer getSpotsLeft() {
        return spotsLeft;
    }

    public void setSpotsLeft(Integer spotsLeft) {
        this.spotsLeft = spotsLeft;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public BigDecimal getPriceAdult() {
        return priceAdult;
    }

    public void setPriceAdult(BigDecimal priceAdult) {
        this.priceAdult = priceAdult;
    }

    public BigDecimal getPriceChild() {
        return priceChild;
    }

    public void setPriceChild(BigDecimal priceChild) {
        this.priceChild = priceChild;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}

