package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.RoomTypeDTO;
import com.duong.travelweb.model.entity.RoomTypeEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoomTypeDTOConverter {

    /** Số phòng và tiện ích được truyền vào (đã lấy theo lô) để tránh N+1. */
    public RoomTypeDTO toRoomTypeDTO(RoomTypeEntity entity, int totalRooms, List<String> amenities) {
        RoomTypeDTO dto = new RoomTypeDTO();
        dto.setId(entity.getId());
        dto.setHotelId(entity.getHotel() != null ? entity.getHotel().getId() : null);
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setMaxOccupancy(entity.getMaxOccupancy());
        dto.setBedType(entity.getBedType());
        dto.setAreaSqM(entity.getAreaSqM());
        dto.setPricePerNight(entity.getPricePerNight());
        dto.setCoverImageUrl(entity.getCoverImageUrl());
        dto.setTotalRooms(totalRooms);
        dto.setAmenities(amenities != null ? amenities : new ArrayList<>());
        return dto;
    }
}
