package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Tình trạng xe trong một khoảng thời gian. */
public class CarAvailabilityDTO {
    private UUID carId;

    private LocalDateTime from;

    private LocalDateTime to;

    private Boolean available;

    private Integer rentalDays;

    private BigDecimal totalPrice;

    private String currencyCode;

    private List<CarBusyRangeDTO> busyRanges;

    public UUID getCarId() {
        return carId;
    }

    public void setCarId(UUID carId) {
        this.carId = carId;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Integer getRentalDays() {
        return rentalDays;
    }

    public void setRentalDays(Integer rentalDays) {
        this.rentalDays = rentalDays;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public List<CarBusyRangeDTO> getBusyRanges() {
        return busyRanges;
    }

    public void setBusyRanges(List<CarBusyRangeDTO> busyRanges) {
        this.busyRanges = busyRanges;
    }
}

