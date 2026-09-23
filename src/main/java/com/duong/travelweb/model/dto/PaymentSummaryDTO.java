package com.duong.travelweb.model.dto;

import java.math.BigDecimal;

public class PaymentSummaryDTO {
    private Long totalTransactions;
    private BigDecimal totalPaid;
    private BigDecimal totalRefunded;
    private String currencyCode;

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public BigDecimal getTotalRefunded() {
        return totalRefunded;
    }

    public void setTotalRefunded(BigDecimal totalRefunded) {
        this.totalRefunded = totalRefunded;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
