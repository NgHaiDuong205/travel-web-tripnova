package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** Một khối ghế cùng hạng / giá để sinh sơ đồ (hàng fromRow..toRow, mỗi hàng các chữ cái letters). */
public class SeatBlockRequestDTO {
    @NotBlank(message = "Thiếu hạng ghế")
    private String seatClass;

    @NotNull(message = "Thiếu hàng bắt đầu")
    @Min(value = 1, message = "Hàng phải từ 1")
    private Integer fromRow;

    @NotNull(message = "Thiếu hàng kết thúc")
    @Min(value = 1, message = "Hàng phải từ 1")
    private Integer toRow;

    @NotBlank(message = "Thiếu các chữ cái ghế")
    @Pattern(regexp = "^[A-Z]{1,10}$", message = "Chữ cái ghế chỉ gồm A-Z, tối đa 10")
    private String letters;

    @NotNull(message = "Thiếu giá ghế")
    @DecimalMin(value = "0.01", message = "Giá ghế phải lớn hơn 0")
    private BigDecimal price;

    public String getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    public Integer getFromRow() {
        return fromRow;
    }

    public void setFromRow(Integer fromRow) {
        this.fromRow = fromRow;
    }

    public Integer getToRow() {
        return toRow;
    }

    public void setToRow(Integer toRow) {
        this.toRow = toRow;
    }

    public String getLetters() {
        return letters;
    }

    public void setLetters(String letters) {
        this.letters = letters;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}

