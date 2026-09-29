package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.util.List;

/** Giá trị có thật để dựng bộ lọc chuyến bay. */
public class FlightFilterOptionsDTO {
    private List<String> airlines;

    private List<String> cabinClasses;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    private String currencyCode;

    public List<String> getAirlines() {
        return airlines;
    }

    public void setAirlines(List<String> airlines) {
        this.airlines = airlines;
    }

    public List<String> getCabinClasses() {
        return cabinClasses;
    }

    public void setCabinClasses(List<String> cabinClasses) {
        this.cabinClasses = cabinClasses;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}

