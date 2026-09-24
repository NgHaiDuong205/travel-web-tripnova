package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.RoomTypeDTOConverter;
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
    private final RoomTypeDTOConverter roomTypeDTOConverter;

    public RoomTypeServiceImpl(RoomTypeRepository roomTypeRepository,
                               RoomRepository roomRepository,
                               RoomTypeDTOConverter roomTypeDTOConverter) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.roomTypeDTOConverter = roomTypeDTOConverter;
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

        return roomTypes.stream()
                .map(rt -> roomTypeDTOConverter.toRoomTypeDTO(rt,
                        roomCounts.getOrDefault(rt.getId(), 0),
                        amenities.getOrDefault(rt.getId(), new ArrayList<>())))
                .toList();
    }
}
