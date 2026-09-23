package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RevenuePointDTO {
    private LocalDate date;
    private BigDecimal revenue;
    private Long transactions;

    public RevenuePointDTO() {
    }

    public RevenuePointDTO(LocalDate date, BigDecimal revenue, Long transactions) {
        this.date = date;
        this.revenue = revenue;
        this.transactions = transactions;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public Long getTransactions() {
        return transactions;
    }

    public void setTransactions(Long transactions) {
        this.transactions = transactions;
    }
}
