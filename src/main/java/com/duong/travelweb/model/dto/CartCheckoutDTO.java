package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Kết quả thanh toán giỏ: 1 order, nhiều booking, 1 payment. */
public class CartCheckoutDTO {
    private UUID orderId;
    private String orderCode;
    private List<UUID> bookingIds = new ArrayList<>();
    private UUID paymentId;
    private BigDecimal amount;
    private String currencyCode;
    private String paymentUrl;
    private LocalDateTime holdExpiresAt;
    /** hotel | car | flight | tour — FE chọn trang thanh toán (/payment hoặc /trip-payment). */
    private String bookingType;

    public String getBookingType() {
        return bookingType;
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }

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

    public List<UUID> getBookingIds() {
        return bookingIds;
    }

    public void setBookingIds(List<UUID> bookingIds) {
        this.bookingIds = bookingIds;
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

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }
}
