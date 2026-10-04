package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lượt đặt trọn gói của lịch trình. state: pending | paid | partial | cancelled | none. */
public class TripBookingDTO {
    private UUID itineraryId;
    private String itineraryStatus;
    private UUID batchId;
    private String state;
    private String currencyCode;
    private BigDecimal totalAmount;
    private LocalDateTime holdExpiresAt;
    private boolean mockPaymentEnabled;
    private List<TripBookingOrderDTO> orders = new ArrayList<>();

    public UUID getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(UUID itineraryId) {
        this.itineraryId = itineraryId;
    }

    public String getItineraryStatus() {
        return itineraryStatus;
    }

    public void setItineraryStatus(String itineraryStatus) {
        this.itineraryStatus = itineraryStatus;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public boolean isMockPaymentEnabled() {
        return mockPaymentEnabled;
    }

    public void setMockPaymentEnabled(boolean mockPaymentEnabled) {
        this.mockPaymentEnabled = mockPaymentEnabled;
    }

    public List<TripBookingOrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<TripBookingOrderDTO> orders) {
        this.orders = orders;
    }
}

