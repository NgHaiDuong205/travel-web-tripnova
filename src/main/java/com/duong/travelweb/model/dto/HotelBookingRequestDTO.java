package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class HotelBookingRequestDTO {
    @NotNull(message = "Thiếu khách sạn")
    private UUID hotelId;

    @NotNull(message = "Thiếu loại phòng")
    private UUID roomTypeId;

    /** Không bắt buộc — bỏ trống thì hệ thống tự chọn phòng trống. */
    private UUID roomId;

    @NotNull(message = "Thiếu ngày nhận phòng")
    private LocalDate checkIn;

    @NotNull(message = "Thiếu ngày trả phòng")
    private LocalDate checkOut;

    @NotNull(message = "Thiếu số người lớn")
    @Min(value = 1, message = "Cần ít nhất 1 người lớn")
    @Max(value = 20, message = "Tối đa 20 người lớn")
    private Integer adults;

    @Min(value = 0, message = "Số trẻ em không hợp lệ")
    @Max(value = 20, message = "Tối đa 20 trẻ em")
    private Integer children = 0;

    @Size(max = 1000, message = "Yêu cầu đặc biệt tối đa 1000 ký tự")
    private String specialRequests;

    @NotBlank(message = "Thiếu phương thức thanh toán")
    @Pattern(regexp = "credit_card|debit_card|bank_transfer|e_wallet|momo|zalopay|vnpay|cash",
            message = "Phương thức thanh toán không hợp lệ")
    private String paymentMethod;

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

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
