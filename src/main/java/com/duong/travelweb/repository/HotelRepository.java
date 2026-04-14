package com.duong.travelweb.repository;

import com.duong.travelweb.builder.HotelSearchBuider;
import com.duong.travelweb.repository.entity.HotelEntity;

import java.util.List;
import java.util.Map;

public interface HotelRepository {
    List<HotelEntity> findHotel(HotelSearchBuider hotelSearchBuider);
}
