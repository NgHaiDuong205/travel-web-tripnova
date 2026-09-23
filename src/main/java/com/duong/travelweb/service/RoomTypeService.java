package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.RoomTypeDTO;

import java.util.List;
import java.util.UUID;

public interface RoomTypeService {
    List<RoomTypeDTO> findByHotelId(UUID hotelId);
}
