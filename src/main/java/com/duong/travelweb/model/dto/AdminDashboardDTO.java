package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardDTO {
    private Long totalUsers;
    private Long totalHotels;
    private Long activeHotels;
    private Long totalBookings;
    private Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
    private BigDecimal totalRevenue;
    private String currencyCode;
    private Long newContactMessages;
    /** Doanh thu 30 ngày gần nhất, đủ từng ngày (ngày không có giao dịch = 0). */
    private List<RevenuePointDTO> revenueByDay = new ArrayList<>();
    private List<HotelBookingDTO> recentBookings = new ArrayList<>();

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getTotalHotels() {
        return totalHotels;
    }

    public void setTotalHotels(Long totalHotels) {
        this.totalHotels = totalHotels;
    }

    public Long getActiveHotels() {
        return activeHotels;
    }

    public void setActiveHotels(Long activeHotels) {
        this.activeHotels = activeHotels;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Map<String, Long> getBookingsByStatus() {
        return bookingsByStatus;
    }

    public void setBookingsByStatus(Map<String, Long> bookingsByStatus) {
        this.bookingsByStatus = bookingsByStatus;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Long getNewContactMessages() {
        return newContactMessages;
    }

    public void setNewContactMessages(Long newContactMessages) {
        this.newContactMessages = newContactMessages;
    }

    public List<RevenuePointDTO> getRevenueByDay() {
        return revenueByDay;
    }

    public void setRevenueByDay(List<RevenuePointDTO> revenueByDay) {
        this.revenueByDay = revenueByDay;
    }

    public List<HotelBookingDTO> getRecentBookings() {
        return recentBookings;
    }

    public void setRecentBookings(List<HotelBookingDTO> recentBookings) {
        this.recentBookings = recentBookings;
    }
}
