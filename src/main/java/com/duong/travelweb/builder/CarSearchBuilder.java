package com.duong.travelweb.builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Bộ lọc danh sách xe. Trường null / rỗng = bỏ qua. */
public class CarSearchBuilder {
    private final UUID destinationId;
    private final String keyword;
    private final String carType;
    private final List<String> brands;
    private final Integer minSeats;
    private final String transmission;
    private final String fuelType;
    private final Boolean withDriver;
    private final BigDecimal priceMin;
    private final BigDecimal priceMax;
    /** Chỉ lấy xe còn trống trong [availableFrom, availableTo) (cả hai phải có). */
    private final LocalDateTime availableFrom;
    private final LocalDateTime availableTo;
    /** true = chỉ xe đang cho thuê (public); null = tất cả (admin). */
    private final Boolean active;
    /** newest | price_asc | price_desc | seats_desc | name */
    private final String sort;

    private CarSearchBuilder(Builder builder) {
        this.destinationId = builder.destinationId;
        this.keyword = builder.keyword;
        this.carType = builder.carType;
        this.brands = builder.brands;
        this.minSeats = builder.minSeats;
        this.transmission = builder.transmission;
        this.fuelType = builder.fuelType;
        this.withDriver = builder.withDriver;
        this.priceMin = builder.priceMin;
        this.priceMax = builder.priceMax;
        this.availableFrom = builder.availableFrom;
        this.availableTo = builder.availableTo;
        this.active = builder.active;
        this.sort = builder.sort;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCarType() {
        return carType;
    }

    public List<String> getBrands() {
        return brands;
    }

    public Integer getMinSeats() {
        return minSeats;
    }

    public String getTransmission() {
        return transmission;
    }

    public String getFuelType() {
        return fuelType;
    }

    public Boolean getWithDriver() {
        return withDriver;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public LocalDateTime getAvailableTo() {
        return availableTo;
    }

    public Boolean getActive() {
        return active;
    }

    public String getSort() {
        return sort;
    }

    public static class Builder {
        private UUID destinationId;
        private String keyword;
        private String carType;
        private List<String> brands;
        private Integer minSeats;
        private String transmission;
        private String fuelType;
        private Boolean withDriver;
        private BigDecimal priceMin;
        private BigDecimal priceMax;
        private LocalDateTime availableFrom;
        private LocalDateTime availableTo;
        private Boolean active;
        private String sort;

        public Builder destinationId(UUID destinationId) {
            this.destinationId = destinationId;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder carType(String carType) {
            this.carType = carType;
            return this;
        }

        public Builder brands(List<String> brands) {
            this.brands = brands;
            return this;
        }

        public Builder minSeats(Integer minSeats) {
            this.minSeats = minSeats;
            return this;
        }

        public Builder transmission(String transmission) {
            this.transmission = transmission;
            return this;
        }

        public Builder fuelType(String fuelType) {
            this.fuelType = fuelType;
            return this;
        }

        public Builder withDriver(Boolean withDriver) {
            this.withDriver = withDriver;
            return this;
        }

        public Builder priceMin(BigDecimal priceMin) {
            this.priceMin = priceMin;
            return this;
        }

        public Builder priceMax(BigDecimal priceMax) {
            this.priceMax = priceMax;
            return this;
        }

        public Builder availableFrom(LocalDateTime availableFrom) {
            this.availableFrom = availableFrom;
            return this;
        }

        public Builder availableTo(LocalDateTime availableTo) {
            this.availableTo = availableTo;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public CarSearchBuilder build() {
            return new CarSearchBuilder(this);
        }
    }
}
