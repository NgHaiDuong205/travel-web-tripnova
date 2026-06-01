package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.RoomDTOConverter;
import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.model.entity.RoomEntity;
import com.duong.travelweb.repository.RoomRepository;
import com.duong.travelweb.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
        List<RoomDTO> result = new ArrayList<>();
        for (RoomEntity item : roomEntities) {
            result.add(roomDTOConverter.toRoomDTO(item));
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
