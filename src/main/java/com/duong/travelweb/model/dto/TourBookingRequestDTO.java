package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Body đặt tour. */
public class TourBookingRequestDTO {
    @NotNull(message = "Thiếu tour")
    private UUID tourId;

    @NotNull(message = "Vui lòng chọn ngày khởi hành")
    private LocalDate departureDate;

    @NotNull(message = "Thiếu số người lớn")
    @Min(value = 1, message = "Cần ít nhất 1 người lớn")
    @Max(value = 50, message = "Tối đa 50 người lớn")
    private Integer numAdults;

    @Min(value = 0, message = "Số trẻ em không hợp lệ")
    @Max(value = 50, message = "Tối đa 50 trẻ em")
    private Integer numChildren;

    @NotBlank(message = "Vui lòng nhập tên người liên hệ")
    @Size(max = 150, message = "Tên tối đa 150 ký tự")
    private String contactName;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String contactPhone;

    @Email(message = "Email liên hệ không hợp lệ")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String contactEmail;

    @Size(max = 1000, message = "Yêu cầu đặc biệt tối đa 1000 ký tự")
    private String specialRequests;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    public UUID getTourId() {
        return tourId;
    }

    public void setTourId(UUID tourId) {
        this.tourId = tourId;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public Integer getNumAdults() {
        return numAdults;
    }

    public void setNumAdults(Integer numAdults) {
        this.numAdults = numAdults;
    }

    public Integer getNumChildren() {
        return numChildren;
    }

    public void setNumChildren(Integer numChildren) {
        this.numChildren = numChildren;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
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

