package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** 1 kết quả tìm kiếm (hotel | destination | tour | car | flight). */
public class SearchResultDTO {
    private String type;
    private UUID id;
    private String title;
    private String subtitle;
    private String imageUrl;
    /** Giá "từ" (hotel: giá phòng thấp nhất/đêm, tour: người lớn, car: /ngày, flight: giá cơ bản); null nếu không có. */
    private BigDecimal price;
    private String priceUnit;
    private Double score;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
