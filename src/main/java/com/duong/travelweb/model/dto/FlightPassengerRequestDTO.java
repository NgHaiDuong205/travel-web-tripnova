package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Một hành khách trong yêu cầu đặt vé. */
public class FlightPassengerRequestDTO {
    @NotBlank(message = "Thiếu tên hành khách")
    @Size(max = 150, message = "Tên tối đa 150 ký tự")
    private String passengerName;

    @Email(message = "Email hành khách không hợp lệ")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String passengerEmail;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String passengerPhone;

    @Size(max = 30, message = "Số hộ chiếu tối đa 30 ký tự")
    private String passportNo;

    @NotNull(message = "Vui lòng chọn ghế cho từng hành khách")
    private UUID seatId;

    @Min(value = 0, message = "Hành lý không hợp lệ")
    @Max(value = 60, message = "Hành lý tối đa 60kg")
    private Integer baggageKg;

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerEmail() {
        return passengerEmail;
    }

    public void setPassengerEmail(String passengerEmail) {
        this.passengerEmail = passengerEmail;
    }

    public String getPassengerPhone() {
        return passengerPhone;
    }

    public void setPassengerPhone(String passengerPhone) {
        this.passengerPhone = passengerPhone;
    }

    public String getPassportNo() {
        return passportNo;
    }

    public void setPassportNo(String passportNo) {
        this.passportNo = passportNo;
    }

    public UUID getSeatId() {
        return seatId;
    }

    public void setSeatId(UUID seatId) {
        this.seatId = seatId;
    }

    public Integer getBaggageKg() {
        return baggageKg;
    }

    public void setBaggageKg(Integer baggageKg) {
        this.baggageKg = baggageKg;
    }
}

