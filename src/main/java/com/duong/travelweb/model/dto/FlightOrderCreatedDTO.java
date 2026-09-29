package com.duong.travelweb.model.dto;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Kết quả đặt vé: một order + payment cho tất cả hành khách. */
public class FlightOrderCreatedDTO {
    private UUID orderId;

    private String orderCode;

    private UUID paymentId;

    private BigDecimal amount;

    private String currencyCode;

    private LocalDateTime holdExpiresAt;

    private List<FlightBookingDTO> bookings;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public List<FlightBookingDTO> getBookings() {
        return bookings;
    }

    public void setBookings(List<FlightBookingDTO> bookings) {
        this.bookings = bookings;
    }
}

