package com.duong.travelweb.model.dto;

import java.math.BigDecimal;

/** Một lát cắt thống kê. */
public class StatSliceDTO {
    private String key;
    private String label;
    private long count;
    private BigDecimal amount;

    public StatSliceDTO() {
    }

    public StatSliceDTO(String key, String label, long count, BigDecimal amount) {
        this.key = key;
        this.label = label;
        this.count = count;
        this.amount = amount;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
