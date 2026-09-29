package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Thông tin khi gắn khách sạn / xe / chuyến bay vào tour (trường nào không dùng thì bỏ trống). */
public class TourLinkRequestDTO {
    @Min(value = 1, message = "Ngày nhận phòng phải từ 1")
    private Integer checkInDay;

    @Min(value = 1, message = "Số đêm phải từ 1")
    @Max(value = 60, message = "Số đêm tối đa 60")
    private Integer nights;

    @Min(value = 1, message = "Ngày dùng xe phải từ 1")
    private Integer usageDay;

    @Size(max = 20, message = "Chặng bay tối đa 20 ký tự")
    private String leg;

    public Integer getCheckInDay() {
        return checkInDay;
    }

    public void setCheckInDay(Integer checkInDay) {
        this.checkInDay = checkInDay;
    }

    public Integer getNights() {
        return nights;
    }

    public void setNights(Integer nights) {
        this.nights = nights;
    }

    public Integer getUsageDay() {
        return usageDay;
    }

    public void setUsageDay(Integer usageDay) {
        this.usageDay = usageDay;
    }

    public String getLeg() {
        return leg;
    }

    public void setLeg(String leg) {
        this.leg = leg;
    }
}

