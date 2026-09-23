package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.model.entity.RoomEntity;
import com.duong.travelweb.model.entity.RoomTypeEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomDTOConverter {
    public RoomDTO toRoomDTO(RoomEntity entity) {
        return toRoomDTO(entity, null);
    }

    /**
     * Prefer this overload when converting a list of rooms: pass amenities pre-fetched in a
     * single batch query (by room type id) to avoid triggering the lazy roomTypeAmenities
     * collection per room (N+1).
     */
    public RoomDTO toRoomDTO(RoomEntity entity, java.util.List<String> amenities) {
        RoomDTO dto = new RoomDTO();
        dto.setId(entity.getId());
        dto.setRoomNumber(entity.getRoomNumber());
        dto.setFloor(entity.getFloor());
        dto.setStatus(entity.getStatus());

        RoomTypeEntity roomType = entity.getRoomType();
        if (roomType != null) {
            dto.setRoomTypeId(roomType.getId());
            dto.setRoomTypeName(roomType.getName());
            dto.setPricePerNight(roomType.getPricePerNight());
            dto.setMaxOccupancy(roomType.getMaxOccupancy());
            dto.setBedType(roomType.getBedType());
            dto.setAreaSqM(roomType.getAreaSqM());

            if (amenities != null) {
                dto.setAmenities(amenities);
            } else if (roomType.getRoomTypeAmenities() != null) {
                java.util.List<String> amenityList = new java.util.ArrayList<>();
                for (com.duong.travelweb.model.entity.RoomTypeAmenityEntity rta : roomType.getRoomTypeAmenities()) {
                    if (rta.getAmenity() != null) {
                        amenityList.add(rta.getAmenity().getName());
                    }
                }
                dto.setAmenities(amenityList);
            }
        }

        return dto;
    }
}
