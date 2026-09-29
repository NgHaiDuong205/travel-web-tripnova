package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một mốc thời gian của chuỗi thống kê (date = ngày đầu kỳ).
 */
public class StatisticsPointDTO {
    private LocalDate date;
    private long bookings;
    private long newUsers;
    private BigDecimal revenue;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getBookings() {
        return bookings;
    }

    public void setBookings(long bookings) {
        this.bookings = bookings;
    }

    public long getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(long newUsers) {
        this.newUsers = newUsers;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
