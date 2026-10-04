package com.duong.travelweb.model.dto;

/** Body thanh toán (giả lập) mọi order đang chờ của lượt đặt trọn gói. */
public class TripBookingPayRequestDTO {
    private boolean success = true;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}

