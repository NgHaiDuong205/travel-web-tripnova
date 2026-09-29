package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Body thêm / sửa dòng giỏ hàng (hiện chỉ hỗ trợ hotel). Khi sửa, itemType/hotelId/roomTypeId bị bỏ qua. */
public class CartItemRequestDTO {
    @Pattern(regexp = "^(hotel|tour|car|flight)?$", message = "Loại sản phẩm không hợp lệ")
    private String itemType;

    private UUID hotelId;

    private UUID roomTypeId;

    @NotNull(message = "Thiếu ngày nhận phòng")
    private LocalDate checkIn;

    @NotNull(message = "Thiếu ngày trả phòng")
    private LocalDate checkOut;

    @Min(value = 1, message = "Số phòng tối thiểu là 1")
    @Max(value = 5, message = "Tối đa 5 phòng mỗi dòng")
    private Integer quantity;

    @NotNull(message = "Thiếu số người lớn")
    @Min(value = 1, message = "Cần ít nhất 1 người lớn")
    @Max(value = 20, message = "Tối đa 20 người lớn")
    private Integer adults;

    @Min(value = 0, message = "Số trẻ em không hợp lệ")
    @Max(value = 20, message = "Tối đa 20 trẻ em")
    private Integer children;

    @Size(max = 1000, message = "Yêu cầu đặc biệt tối đa 1000 ký tự")
    private String specialRequests;

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
}
