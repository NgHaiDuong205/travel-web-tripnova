package com.duong.travelweb.builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Bộ lọc tìm chuyến bay. Trường null / rỗng = bỏ qua. */
public class FlightSearchBuilder {
    /** Mã sân bay (khớp chính xác) hoặc tên thành phố (khớp một phần). */
    private final String from;
    private final String to;
    private final LocalDate departDate;
    /** economy | premium_economy | business | first — lọc chuyến còn ghế hạng này. */
    private final String seatClass;
    /** Số hành khách: chuyến phải còn ít nhất chừng này ghế trống (theo hạng nếu có). */
    private final Integer passengers;
    private final List<String> airlines;
    /** Còn ít nhất một ghế trống (theo hạng) giá <= priceMax. */
    private final BigDecimal priceMax;
    /** morning (5-12h) | afternoon (12-17h) | evening (17-21h) | night (21-5h) */
    private final String timeOfDay;
    private final String keyword;
    /** true = chỉ chuyến đang bán và chưa cất cánh (public); null = tất cả (admin). */
    private final Boolean upcomingActiveOnly;
    /** departure | price_asc | price_desc | duration */
    private final String sort;

    private FlightSearchBuilder(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.departDate = builder.departDate;
        this.seatClass = builder.seatClass;
        this.passengers = builder.passengers;
        this.airlines = builder.airlines;
        this.priceMax = builder.priceMax;
        this.timeOfDay = builder.timeOfDay;
        this.keyword = builder.keyword;
        this.upcomingActiveOnly = builder.upcomingActiveOnly;
        this.sort = builder.sort;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public LocalDate getDepartDate() {
        return departDate;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public Integer getPassengers() {
        return passengers;
    }

    public List<String> getAirlines() {
        return airlines;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public String getKeyword() {
        return keyword;
    }

    public Boolean getUpcomingActiveOnly() {
        return upcomingActiveOnly;
    }

    public String getSort() {
        return sort;
    }

    public static class Builder {
        private String from;
        private String to;
        private LocalDate departDate;
        private String seatClass;
        private Integer passengers;
        private List<String> airlines;
        private BigDecimal priceMax;
        private String timeOfDay;
        private String keyword;
        private Boolean upcomingActiveOnly;
        private String sort;

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder departDate(LocalDate departDate) {
            this.departDate = departDate;
            return this;
        }

        public Builder seatClass(String seatClass) {
            this.seatClass = seatClass;
            return this;
        }

        public Builder passengers(Integer passengers) {
            this.passengers = passengers;
            return this;
        }

        public Builder airlines(List<String> airlines) {
            this.airlines = airlines;
            return this;
        }

        public Builder priceMax(BigDecimal priceMax) {
            this.priceMax = priceMax;
            return this;
        }

        public Builder timeOfDay(String timeOfDay) {
            this.timeOfDay = timeOfDay;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder upcomingActiveOnly(Boolean upcomingActiveOnly) {
            this.upcomingActiveOnly = upcomingActiveOnly;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public FlightSearchBuilder build() {
            return new FlightSearchBuilder(this);
        }
    }
}
