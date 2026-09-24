package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Body tạo/sửa phòng (admin). */
public class RoomRequestDTO {
    @NotNull(message = "Thiếu hạng phòng")
    private UUID roomTypeId;

    @NotBlank(message = "Số phòng không được trống")
    @Size(max = 10, message = "Số phòng tối đa 10 ký tự")
    private String roomNumber;

    @Min(value = -5, message = "Tầng không hợp lệ")
    @Max(value = 200, message = "Tầng không hợp lệ")
    private Integer floor;

    @Pattern(regexp = "^(available|occupied|maintenance)$", message = "Trạng thái phải là available, occupied hoặc maintenance")
    private String status;

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(UUID roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
