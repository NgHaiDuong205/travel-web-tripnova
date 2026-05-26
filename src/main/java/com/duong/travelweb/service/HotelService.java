package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.HotelDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HotelService {
    List<HotelDTO> findHotel(Map<String,Object> params, List<String> amenities);
    HotelDTO getHotelById(UUID id);
}
