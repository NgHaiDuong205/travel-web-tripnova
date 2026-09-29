package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.util.List;

/** Các giá trị có thật để dựng bộ lọc xe (chỉ tính xe đang cho thuê). */
public class CarFilterOptionsDTO {
    private List<String> carTypes;
    private List<String> brands;
    private List<String> transmissions;
    private List<String> fuelTypes;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String currencyCode;

    public List<String> getCarTypes() {
        return carTypes;
    }

    public void setCarTypes(List<String> carTypes) {
        this.carTypes = carTypes;
    }

    public List<String> getBrands() {
        return brands;
    }

    public void setBrands(List<String> brands) {
        this.brands = brands;
    }

    public List<String> getTransmissions() {
        return transmissions;
    }

    public void setTransmissions(List<String> transmissions) {
        this.transmissions = transmissions;
    }

    public List<String> getFuelTypes() {
        return fuelTypes;
    }

    public void setFuelTypes(List<String> fuelTypes) {
        this.fuelTypes = fuelTypes;
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
