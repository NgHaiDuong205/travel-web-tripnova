package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Giỏ hàng của user. */
public class CartDTO {
    private UUID id;
    private List<CartItemDTO> items = new ArrayList<>();
    private int itemCount;
    private BigDecimal totalAmount;
    private String currencyCode;
    private boolean hasIssues;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isHasIssues() {
        return hasIssues;
    }

    public void setHasIssues(boolean hasIssues) {
        this.hasIssues = hasIssues;
    }
}
