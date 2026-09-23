package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.RoomDTOConverter;
import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.model.entity.RoomEntity;
import com.duong.travelweb.repository.RoomRepository;
import com.duong.travelweb.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final RoomDTOConverter roomDTOConverter;
    private final com.duong.travelweb.converter.RoomSearchBuilderConverter roomSearchBuilderConverter;

    public RoomServiceImpl(RoomRepository roomRepository, RoomDTOConverter roomDTOConverter, com.duong.travelweb.converter.RoomSearchBuilderConverter roomSearchBuilderConverter) {
        this.roomRepository = roomRepository;
        this.roomDTOConverter = roomDTOConverter;
        this.roomSearchBuilderConverter = roomSearchBuilderConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> findRoomsByHotelId(UUID hotelId) {
        List<RoomEntity> roomEntities = roomRepository.findByHotelId(hotelId);
        List<RoomDTO> result = new ArrayList<>();
        for (RoomEntity item : roomEntities) {
            result.add(roomDTOConverter.toRoomDTO(item));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> findRoomsByHotelId(UUID hotelId, java.util.Map<String, Object> params, List<String> amenities) {
        com.duong.travelweb.builder.RoomSearchBuilder searchBuilder = roomSearchBuilderConverter.toRoomSearchBuilder(params, amenities);
        List<RoomEntity> roomEntities = roomRepository.searchRooms(hotelId, searchBuilder);

        java.util.Map<UUID, String> roomStatusMap = new java.util.HashMap<>();
        if (searchBuilder.getCheckIn() != null && searchBuilder.getCheckOut() != null) {
            List<Object[]> occupiedRooms = roomRepository.findBookedOrBlockedRoomStatuses(
                hotelId, searchBuilder.getCheckIn(), searchBuilder.getCheckOut()
            );
            for (Object[] row : occupiedRooms) {
                UUID roomId = (UUID) row[0];
                String status = (String) row[1];
                if (!roomStatusMap.containsKey(roomId) || "blocked".equals(status)) {
                    roomStatusMap.put(roomId, status);
                }
            }
        }

        Set<UUID> roomTypeIds = new LinkedHashSet<>();
        for (RoomEntity item : roomEntities) {
            if (item.getRoomType() != null) {
                roomTypeIds.add(item.getRoomType().getId());
            }
        }
        java.util.Map<UUID, List<String>> amenitiesByRoomType = new java.util.HashMap<>();
        if (!roomTypeIds.isEmpty()) {
            List<Object[]> amenityRows = roomRepository.findAmenityNamesByRoomTypeIds(new ArrayList<>(roomTypeIds));
            for (Object[] row : amenityRows) {
                UUID roomTypeId = (UUID) row[0];
                String amenityName = (String) row[1];
                amenitiesByRoomType.computeIfAbsent(roomTypeId, k -> new ArrayList<>()).add(amenityName);
            }
        }

        List<RoomDTO> result = new ArrayList<>();
        for (RoomEntity item : roomEntities) {
            List<String> roomAmenities = item.getRoomType() != null
                ? amenitiesByRoomType.getOrDefault(item.getRoomType().getId(), new ArrayList<>())
                : new ArrayList<>();
            RoomDTO dto = roomDTOConverter.toRoomDTO(item, roomAmenities);
            if (roomStatusMap.containsKey(dto.getId())) {
                dto.setStatus(roomStatusMap.get(dto.getId()));
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDTO findRoomByIdAndHotelId(UUID hotelId, UUID roomId) {
        RoomEntity roomEntity = roomRepository.findByIdAndHotelId(roomId, hotelId).orElse(null);
        if (roomEntity != null) {
            return roomDTOConverter.toRoomDTO(roomEntity);
        }
        return null;
    }
}
