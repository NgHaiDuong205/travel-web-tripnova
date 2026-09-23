package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.RoomTypeDTO;
import com.duong.travelweb.model.entity.RoomTypeEntity;
import com.duong.travelweb.repository.RoomRepository;
import com.duong.travelweb.repository.RoomTypeRepository;
import com.duong.travelweb.service.RoomTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomTypeServiceImpl implements RoomTypeService {
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    public RoomTypeServiceImpl(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeDTO> findByHotelId(UUID hotelId) {
        List<RoomTypeEntity> roomTypes = roomTypeRepository.findActiveByHotelId(hotelId);
        if (roomTypes.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = roomTypes.stream().map(RoomTypeEntity::getId).toList();

        Map<UUID, Integer> roomCounts = new HashMap<>();
        for (Object[] row : roomTypeRepository.countRoomsByRoomTypeIds(ids)) {
            roomCounts.put((UUID) row[0], ((Long) row[1]).intValue());
        }
        Map<UUID, List<String>> amenities = new HashMap<>();
        for (Object[] row : roomRepository.findAmenityNamesByRoomTypeIds(ids)) {
            amenities.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add((String) row[1]);
        }

        return roomTypes.stream().map(rt -> {
            RoomTypeDTO dto = new RoomTypeDTO();
            dto.setId(rt.getId());
            dto.setHotelId(hotelId);
            dto.setName(rt.getName());
            dto.setDescription(rt.getDescription());
            dto.setMaxOccupancy(rt.getMaxOccupancy());
            dto.setBedType(rt.getBedType());
            dto.setAreaSqM(rt.getAreaSqM());
            dto.setPricePerNight(rt.getPricePerNight());
            dto.setCoverImageUrl(rt.getCoverImageUrl());
            dto.setTotalRooms(roomCounts.getOrDefault(rt.getId(), 0));
            dto.setAmenities(amenities.getOrDefault(rt.getId(), new ArrayList<>()));
            return dto;
        }).toList();
    }
}
