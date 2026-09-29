package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Body tạo / sửa một ghế (admin). */
public class FlightSeatRequestDTO {
    @NotBlank(message = "Thiếu số ghế")
    @Size(max = 10, message = "Số ghế tối đa 10 ký tự")
    private String seatNumber;

    @NotBlank(message = "Thiếu hạng ghế")
    private String seatClass;

    @NotNull(message = "Thiếu giá ghế")
    @DecimalMin(value = "0.01", message = "Giá ghế phải lớn hơn 0")
    private BigDecimal price;

    private String status;

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

