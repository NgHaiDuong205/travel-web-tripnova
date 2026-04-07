package com.duong.travelweb.repository;

import com.duong.travelweb.repository.entity.HotelEntity;

import java.util.List;
import java.util.Map;

public interface HotelRepository {
    List<HotelEntity> findHotel(Map<String,Object> params,List<String> typeCode);
}
