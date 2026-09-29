package com.duong.travelweb.model.dto;



/** Sân bay (lấy từ các chuyến bay đang bán). */
public class AirportDTO {
    private String code;

    private String city;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}

