package com.duong.travelweb.model.dto;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Dòng giỏ chuyến đi; giá và tình trạng tính lại mỗi lần đọc. */
public class TripCartItemDTO {
    private UUID id;
    private String itemType;
    /** tourId / carId / flightId */
    private UUID itemId;
    private String title;
    private String subtitle;
    private String imageUrl;
    /** Yêu cầu đặt chỗ đã lưu (body POST /api/{type}-bookings, không có paymentMethod). */
    private JsonNode booking;
    /** Giá lúc thêm vào giỏ. */
    private BigDecimal addedPrice;
    /** Giá hiện tại (null nếu không đặt được nữa). */
    private BigDecimal currentPrice;
    private Boolean priceChanged;
    /** Lý do không đặt được (hết chỗ, quá hạn...), null nếu vẫn đặt được. */
    private String issue;
    private LocalDateTime addedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
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

    public JsonNode getBooking() {
        return booking;
    }

    public void setBooking(JsonNode booking) {
        this.booking = booking;
    }

    public BigDecimal getAddedPrice() {
        return addedPrice;
    }

    public void setAddedPrice(BigDecimal addedPrice) {
        this.addedPrice = addedPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Boolean getPriceChanged() {
        return priceChanged;
    }

    public void setPriceChanged(Boolean priceChanged) {
        this.priceChanged = priceChanged;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
