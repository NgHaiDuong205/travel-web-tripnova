package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardDTO {
    private Long totalBookings;
    private Long upcomingBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private BigDecimal totalSpent;
    private String currencyCode;
    private Integer loyaltyPoints;
    private HotelBookingDTO nextBooking;
    private Long daysUntilNextTrip;
    private List<HotelBookingDTO> recentBookings = new ArrayList<>();

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Long getUpcomingBookings() {
        return upcomingBookings;
    }

    public void setUpcomingBookings(Long upcomingBookings) {
        this.upcomingBookings = upcomingBookings;
    }

    public Long getCompletedBookings() {
        return completedBookings;
    }

    public void setCompletedBookings(Long completedBookings) {
        this.completedBookings = completedBookings;
    }

    public Long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(Long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public HotelBookingDTO getNextBooking() {
        return nextBooking;
    }

    public void setNextBooking(HotelBookingDTO nextBooking) {
        this.nextBooking = nextBooking;
    }

    public Long getDaysUntilNextTrip() {
        return daysUntilNextTrip;
    }

    public void setDaysUntilNextTrip(Long daysUntilNextTrip) {
        this.daysUntilNextTrip = daysUntilNextTrip;
    }

    public List<HotelBookingDTO> getRecentBookings() {
        return recentBookings;
    }

    public void setRecentBookings(List<HotelBookingDTO> recentBookings) {
        this.recentBookings = recentBookings;
    }
}
