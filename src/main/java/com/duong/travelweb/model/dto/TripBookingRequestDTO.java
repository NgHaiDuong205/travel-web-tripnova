package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** Body đặt trọn gói: hạng phòng + số phòng (tuỳ chọn), xe (tuỳ chọn), phương thức thanh toán. */
public class TripBookingRequestDTO {
    private UUID hotelId;
    private UUID roomTypeId;
    @Min(value = 1, message = "Số phòng tối thiểu là 1")
    @Max(value = 20, message = "Tối đa 20 phòng")
    private Integer roomQuantity;
    @Size(max = 500, message = "Yêu cầu đặc biệt tối đa 500 ký tự")
    private String specialRequests;
    private UUID carId;
    private LocalDateTime pickupDate;
    private LocalDateTime returnDate;
    @Size(max = 255, message = "Địa điểm nhận xe tối đa 255 ký tự")
    private String pickupLocation;
    @Size(max = 255, message = "Địa điểm trả xe tối đa 255 ký tự")
    private String returnLocation;
    @Size(max = 50, message = "Số GPLX tối đa 50 ký tự")
    private String driverLicenseNo;
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    @Pattern(regexp = "credit_card|debit_card|bank_transfer|e_wallet|momo|zalopay|vnpay|cash", message = "Phương thức thanh toán không hợp lệ")
    private String paymentMethod;
    @AssertTrue(message = "Vui lòng đồng ý điều khoản và chính sách huỷ")
    private boolean acceptTerms;

    public UUID getHotelId() {
        return hotelId;
    }

    public void setHotelId(UUID hotelId) {
        this.hotelId = hotelId;
    }

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(UUID roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public Integer getRoomQuantity() {
        return roomQuantity;
    }

    public void setRoomQuantity(Integer roomQuantity) {
        this.roomQuantity = roomQuantity;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public UUID getCarId() {
        return carId;
    }

    public void setCarId(UUID carId) {
        this.carId = carId;
    }

    public LocalDateTime getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDateTime pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getReturnLocation() {
        return returnLocation;
    }

    public void setReturnLocation(String returnLocation) {
        this.returnLocation = returnLocation;
    }

    public String getDriverLicenseNo() {
        return driverLicenseNo;
    }

    public void setDriverLicenseNo(String driverLicenseNo) {
        this.driverLicenseNo = driverLicenseNo;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isAcceptTerms() {
        return acceptTerms;
    }

    public void setAcceptTerms(boolean acceptTerms) {
        this.acceptTerms = acceptTerms;
    }
}
