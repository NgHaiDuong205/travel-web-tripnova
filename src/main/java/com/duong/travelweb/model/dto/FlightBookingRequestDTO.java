package com.duong.travelweb.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.util.List;

/** Body đặt vé (nhiều hành khách, cùng một chuyến). */
public class FlightBookingRequestDTO {
    @NotNull(message = "Thiếu chuyến bay")
    private UUID flightId;

    @NotEmpty(message = "Cần ít nhất một hành khách")
    @Size(max = 9, message = "Tối đa 9 hành khách mỗi lần đặt")
    private List<@Valid FlightPassengerRequestDTO> passengers;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    public UUID getFlightId() {
        return flightId;
    }

    public void setFlightId(UUID flightId) {
        this.flightId = flightId;
    }

    public List<@Valid FlightPassengerRequestDTO> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<@Valid FlightPassengerRequestDTO> passengers) {
        this.passengers = passengers;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

