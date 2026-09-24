package com.duong.travelweb.model.dto;

/** Tiện ích cho màn admin: kèm số khách sạn / hạng phòng đang dùng. */
public class AdminAmenityDTO extends AmenityDTO {
    private long hotelCount;
    private long roomTypeCount;

    public long getHotelCount() {
        return hotelCount;
    }

    public void setHotelCount(long hotelCount) {
        this.hotelCount = hotelCount;
    }

    public long getRoomTypeCount() {
        return roomTypeCount;
    }

    public void setRoomTypeCount(long roomTypeCount) {
        this.roomTypeCount = roomTypeCount;
    }
}
