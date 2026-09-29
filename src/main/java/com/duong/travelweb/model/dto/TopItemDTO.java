package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một dòng xếp hạng (top khách sạn / điểm đến) trên dashboard.
 */
public class TopItemDTO {
    private UUID id;
    private String name;
    private String subtitle;
    private long bookings;
    private long nights;
    private BigDecimal revenue;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public long getBookings() {
        return bookings;
    }

    public void setBookings(long bookings) {
        this.bookings = bookings;
    }

    public long getNights() {
        return nights;
    }

    public void setNights(long nights) {
        this.nights = nights;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
