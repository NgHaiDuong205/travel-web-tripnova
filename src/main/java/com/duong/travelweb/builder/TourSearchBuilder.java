package com.duong.travelweb.builder;

import java.math.BigDecimal;
import java.util.UUID;

/** Bộ lọc danh sách tour. Trường null = bỏ qua. */
public class TourSearchBuilder {
    private final String keyword;
    private final UUID destinationId;
    private final BigDecimal priceMin;
    private final BigDecimal priceMax;
    private final Integer minDays;
    private final Integer maxDays;
    /** true = chỉ tour đang bán (public); null = tất cả (admin). */
    private final Boolean active;
    /** newest | price_asc | price_desc | duration_asc | duration_desc | departure */
    private final String sort;

    private TourSearchBuilder(Builder builder) {
        this.keyword = builder.keyword;
        this.destinationId = builder.destinationId;
        this.priceMin = builder.priceMin;
        this.priceMax = builder.priceMax;
        this.minDays = builder.minDays;
        this.maxDays = builder.maxDays;
        this.active = builder.active;
        this.sort = builder.sort;
    }

    public String getKeyword() {
        return keyword;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public Integer getMinDays() {
        return minDays;
    }

    public Integer getMaxDays() {
        return maxDays;
    }

    public Boolean getActive() {
        return active;
    }

    public String getSort() {
        return sort;
    }

    public static class Builder {
        private String keyword;
        private UUID destinationId;
        private BigDecimal priceMin;
        private BigDecimal priceMax;
        private Integer minDays;
        private Integer maxDays;
        private Boolean active;
        private String sort;

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder destinationId(UUID destinationId) {
            this.destinationId = destinationId;
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

        public Builder minDays(Integer minDays) {
            this.minDays = minDays;
            return this;
        }

        public Builder maxDays(Integer maxDays) {
            this.maxDays = maxDays;
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

        public TourSearchBuilder build() {
            return new TourSearchBuilder(this);
        }
    }
}
