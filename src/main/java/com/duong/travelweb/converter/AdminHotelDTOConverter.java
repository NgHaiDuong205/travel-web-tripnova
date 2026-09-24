package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.AdminHotelDTO;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Map tay (không dùng ModelMapper) vì HotelEntity và DestinationEntity trùng tên nhiều trường (latitude, name...). */
@Component
public class AdminHotelDTOConverter {

    public AdminHotelDTO toAdminHotelDTO(HotelEntity entity, List<String> amenities, List<UUID> amenityIds, int roomTypeCount) {
        AdminHotelDTO dto = new AdminHotelDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setAddress(entity.getAddress());
        dto.setStarRating(entity.getStarRating());
        dto.setCheckInTime(entity.getCheckInTime());
        dto.setCheckOutTime(entity.getCheckOutTime());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setCancellationPolicy(entity.getCancellationPolicy());
        dto.setCancellationHours(entity.getCancellationHours());
        dto.setBreakfastIncluded(entity.getBreakfastIncluded());
        dto.setPetFriendly(entity.getPetFriendly());
        dto.setGooglePlaceId(entity.getGooglePlaceId());
        dto.setTotalRooms(entity.getTotalRooms() != null ? entity.getTotalRooms() : 0);
        dto.setCoverImageUrl(entity.getCoverImageUrl());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        DestinationEntity destination = entity.getDestination();
        if (destination != null) {
            dto.setDestinationId(destination.getId());
            dto.setDestinationName(destination.getName());
            if (destination.getCountry() != null) {
                dto.setCountryName(destination.getCountry().getName());
            }
        }
        UserEntity manager = entity.getManagedBy();
        if (manager != null) {
            dto.setManagedById(manager.getId());
            dto.setManagedByName(manager.getFullName());
        }
        dto.setAmenities(amenities != null ? amenities : new ArrayList<>());
        dto.setAmenityIds(amenityIds != null ? amenityIds : new ArrayList<>());
        dto.setRoomTypeCount(roomTypeCount);
        return dto;
    }
}
