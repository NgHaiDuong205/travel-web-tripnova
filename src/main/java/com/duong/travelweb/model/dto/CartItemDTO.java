package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Dòng giỏ hàng; giá và tình trạng phòng được tính lại tại thời điểm đọc. */
public class CartItemDTO {
    private UUID id;
    private String itemType;
    private UUID hotelId;
    private String hotelName;
    private String hotelCoverImageUrl;
    private UUID roomTypeId;
    private String roomTypeName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer nights;
    private int quantity;
    private Integer adults;
    private Integer children;
    private String specialRequests;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal snapshotUnitPrice;
    private boolean priceChanged;
    private Integer availableRooms;
    /** null = đặt được; expired | inactive | unavailable (không đủ phòng) */
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

    public UUID getHotelId() {
        return hotelId;
    }

    public void setHotelId(UUID hotelId) {
        this.hotelId = hotelId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getHotelCoverImageUrl() {
        return hotelCoverImageUrl;
    }

    public void setHotelCoverImageUrl(String hotelCoverImageUrl) {
        this.hotelCoverImageUrl = hotelCoverImageUrl;
    }

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(UUID roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public Integer getNights() {
        return nights;
    }

    public void setNights(Integer nights) {
        this.nights = nights;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Integer getAdults() {
        return adults;
    }

    public void setAdults(Integer adults) {
        this.adults = adults;
    }

    public Integer getChildren() {
        return children;
    }

    public void setChildren(Integer children) {
        this.children = children;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getSnapshotUnitPrice() {
        return snapshotUnitPrice;
    }

    public void setSnapshotUnitPrice(BigDecimal snapshotUnitPrice) {
        this.snapshotUnitPrice = snapshotUnitPrice;
    }

    public boolean isPriceChanged() {
        return priceChanged;
    }

    public void setPriceChanged(boolean priceChanged) {
        this.priceChanged = priceChanged;
    }

    public Integer getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(Integer availableRooms) {
        this.availableRooms = availableRooms;
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
