package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AmenityDTO;

import java.util.List;

public interface AmenityService {
    List<AmenityDTO> findAmenities(String category);
}
