package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.RoomDTO;

import java.util.List;
import java.util.UUID;

import java.util.Map;

public interface RoomService {
    List<RoomDTO> findRoomsByHotelId(UUID hotelId);
    List<RoomDTO> findRoomsByHotelId(UUID hotelId, Map<String, Object> params, List<String> amenities);
    RoomDTO findRoomByIdAndHotelId(UUID hotelId, UUID roomId);
}
