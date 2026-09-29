package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thống kê admin trong khoảng [from, to], kèm so sánh với kỳ liền trước cùng độ dài.
 */
public class DashboardStatisticsDTO {
    private LocalDate from;
    private LocalDate to;
    private String granularity;
    private String currencyCode;
    private long newUsers;
    private long totalBookings;
    private Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
    private BigDecimal revenue;
    private long transactions;
    private BigDecimal refundedAmount;
    private BigDecimal averageBookingValue;
    private long previousNewUsers;
    private long previousBookings;
    private BigDecimal previousRevenue;
    private List<StatisticsPointDTO> series = new ArrayList<>();

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public long getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(long newUsers) {
        this.newUsers = newUsers;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Map<String, Long> getBookingsByStatus() {
        return bookingsByStatus;
    }

    public void setBookingsByStatus(Map<String, Long> bookingsByStatus) {
        this.bookingsByStatus = bookingsByStatus;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public long getTransactions() {
        return transactions;
    }

    public void setTransactions(long transactions) {
        this.transactions = transactions;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public BigDecimal getAverageBookingValue() {
        return averageBookingValue;
    }

    public void setAverageBookingValue(BigDecimal averageBookingValue) {
        this.averageBookingValue = averageBookingValue;
    }

    public long getPreviousNewUsers() {
        return previousNewUsers;
    }

    public void setPreviousNewUsers(long previousNewUsers) {
        this.previousNewUsers = previousNewUsers;
    }

    public long getPreviousBookings() {
        return previousBookings;
    }

    public void setPreviousBookings(long previousBookings) {
        this.previousBookings = previousBookings;
    }

    public BigDecimal getPreviousRevenue() {
        return previousRevenue;
    }

    public void setPreviousRevenue(BigDecimal previousRevenue) {
        this.previousRevenue = previousRevenue;
    }

    public List<StatisticsPointDTO> getSeries() {
        return series;
    }

    public void setSeries(List<StatisticsPointDTO> series) {
        this.series = series;
    }
}
