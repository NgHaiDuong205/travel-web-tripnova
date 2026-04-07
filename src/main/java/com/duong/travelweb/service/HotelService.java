package com.duong.travelweb.service;

import com.duong.travelweb.model.HotelDTO;

import java.util.List;
import java.util.Map;

public interface HotelService {
    List<HotelDTO> findHotel(Map<String,Object> params,List<String> typeCode);
}
