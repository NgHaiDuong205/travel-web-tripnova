package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.HotelDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HotelService {
    List<HotelDTO> findHotel(Map<String,Object> params, List<String> amenities);
    long countHotel(Map<String,Object> params, List<String> amenities);
    HotelDTO getHotelById(UUID id, Map<String, Object> params);
    /** Khách sạn trong bán kính quanh (lat, lng), gần nhất trước, kèm distanceKm. */
    List<HotelDTO> findNearby(double lat, double lng, double radiusKm, int limit);
}
