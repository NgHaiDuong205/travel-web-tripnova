package com.duong.travelweb.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Khách sạn trong báo giá đặt trọn gói: thông tin, chính sách huỷ, các hạng phòng. */
public class TripHotelQuoteDTO {
    private UUID hotelId;
    private String name;
    private String address;
    private Integer starRating;
    private String coverImageUrl;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String checkInTime;
    private String checkOutTime;
    private int nights;
    private Boolean breakfastIncluded;
    private String cancellationPolicy;
    private Integer cancellationHours;
    private LocalDateTime freeCancellationUntil;
    private UUID recommendedRoomTypeId;
    private List<TripRoomOptionDTO> roomOptions = new ArrayList<>();

    public UUID getHotelId() {
        return hotelId;
    }

    public void setHotelId(UUID hotelId) {
        this.hotelId = hotelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getStarRating() {
        return starRating;
    }

    public void setStarRating(Integer starRating) {
        this.starRating = starRating;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
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

    public String getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(String checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(String checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public Boolean getBreakfastIncluded() {
        return breakfastIncluded;
    }

    public void setBreakfastIncluded(Boolean breakfastIncluded) {
        this.breakfastIncluded = breakfastIncluded;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public Integer getCancellationHours() {
        return cancellationHours;
    }

    public void setCancellationHours(Integer cancellationHours) {
        this.cancellationHours = cancellationHours;
    }

    public LocalDateTime getFreeCancellationUntil() {
        return freeCancellationUntil;
    }

    public void setFreeCancellationUntil(LocalDateTime freeCancellationUntil) {
        this.freeCancellationUntil = freeCancellationUntil;
    }

    public UUID getRecommendedRoomTypeId() {
        return recommendedRoomTypeId;
    }

    public void setRecommendedRoomTypeId(UUID recommendedRoomTypeId) {
        this.recommendedRoomTypeId = recommendedRoomTypeId;
    }

    public List<TripRoomOptionDTO> getRoomOptions() {
        return roomOptions;
    }

    public void setRoomOptions(List<TripRoomOptionDTO> roomOptions) {
        this.roomOptions = roomOptions;
    }
}

