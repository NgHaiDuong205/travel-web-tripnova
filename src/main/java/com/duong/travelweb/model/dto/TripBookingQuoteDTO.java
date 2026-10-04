package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Báo giá đặt trọn gói từ lịch trình: khách sạn + xe (thu ngay) và chi phí dự kiến trả tại chỗ. */
public class TripBookingQuoteDTO {
    private UUID itineraryId;
    private String itineraryTitle;
    private String itineraryStatus;
    private UUID destinationId;
    private String destinationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int partySize;
    private String currencyCode;
    private long holdMinutes;
    private boolean mockPaymentEnabled;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private TripHotelQuoteDTO hotel;
    private String hotelUnavailableReason;
    private List<TripCarOptionDTO> cars = new ArrayList<>();
    private UUID recommendedCarId;
    private LocalDateTime carPickupDate;
    private LocalDateTime carReturnDate;
    private BigDecimal onSiteEstimate;
    private int carFreeCancellationHours;

    public UUID getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(UUID itineraryId) {
        this.itineraryId = itineraryId;
    }

    public String getItineraryTitle() {
        return itineraryTitle;
    }

    public void setItineraryTitle(String itineraryTitle) {
        this.itineraryTitle = itineraryTitle;
    }

    public String getItineraryStatus() {
        return itineraryStatus;
    }

    public void setItineraryStatus(String itineraryStatus) {
        this.itineraryStatus = itineraryStatus;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(UUID destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public long getHoldMinutes() {
        return holdMinutes;
    }

    public void setHoldMinutes(long holdMinutes) {
        this.holdMinutes = holdMinutes;
    }

    public boolean isMockPaymentEnabled() {
        return mockPaymentEnabled;
    }

    public void setMockPaymentEnabled(boolean mockPaymentEnabled) {
        this.mockPaymentEnabled = mockPaymentEnabled;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public TripHotelQuoteDTO getHotel() {
        return hotel;
    }

    public void setHotel(TripHotelQuoteDTO hotel) {
        this.hotel = hotel;
    }

    public String getHotelUnavailableReason() {
        return hotelUnavailableReason;
    }

    public void setHotelUnavailableReason(String hotelUnavailableReason) {
        this.hotelUnavailableReason = hotelUnavailableReason;
    }

    public List<TripCarOptionDTO> getCars() {
        return cars;
    }

    public void setCars(List<TripCarOptionDTO> cars) {
        this.cars = cars;
    }

    public UUID getRecommendedCarId() {
        return recommendedCarId;
    }

    public void setRecommendedCarId(UUID recommendedCarId) {
        this.recommendedCarId = recommendedCarId;
    }

    public LocalDateTime getCarPickupDate() {
        return carPickupDate;
    }

    public void setCarPickupDate(LocalDateTime carPickupDate) {
        this.carPickupDate = carPickupDate;
    }

    public LocalDateTime getCarReturnDate() {
        return carReturnDate;
    }

    public void setCarReturnDate(LocalDateTime carReturnDate) {
        this.carReturnDate = carReturnDate;
    }

    public BigDecimal getOnSiteEstimate() {
        return onSiteEstimate;
    }

    public void setOnSiteEstimate(BigDecimal onSiteEstimate) {
        this.onSiteEstimate = onSiteEstimate;
    }

    public int getCarFreeCancellationHours() {
        return carFreeCancellationHours;
    }

    public void setCarFreeCancellationHours(int carFreeCancellationHours) {
        this.carFreeCancellationHours = carFreeCancellationHours;
    }
}

