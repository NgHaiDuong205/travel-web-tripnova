package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** Body đặt xe. */
public class CarBookingRequestDTO {
    @NotNull(message = "Thiếu xe")
    private UUID carId;

    @NotNull(message = "Thiếu thời gian nhận xe")
    private LocalDateTime pickupDate;

    @NotNull(message = "Thiếu thời gian trả xe")
    private LocalDateTime returnDate;

    @Size(max = 500, message = "Nơi nhận xe tối đa 500 ký tự")
    private String pickupLocation;

    @Size(max = 500, message = "Nơi trả xe tối đa 500 ký tự")
    private String returnLocation;

    @Size(max = 50, message = "Số bằng lái tối đa 50 ký tự")
    private String driverLicenseNo;

    @Size(max = 1000, message = "Yêu cầu đặc biệt tối đa 1000 ký tự")
    private String specialRequests;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

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

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

