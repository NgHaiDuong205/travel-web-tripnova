package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.converter.HotelDTOConverter;
import com.duong.travelweb.converter.HotelSearchBuilderConverter;
import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.RoomRepository;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.service.HotelService;
import com.duong.travelweb.util.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HotelServiceImpl implements HotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelDTOConverter hotelDTOConverter;
    private final HotelSearchBuilderConverter hotelSearchBuilderConverter;

    public HotelServiceImpl(HotelRepository hotelRepository,
                            RoomRepository roomRepository,
                            HotelDTOConverter hotelDTOConverter,
                            HotelSearchBuilderConverter hotelSearchBuilderConverter) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.hotelDTOConverter = hotelDTOConverter;
        this.hotelSearchBuilderConverter = hotelSearchBuilderConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelDTO> findHotel(Map<String, Object> params, List<String> amenities) {
        HotelSearchBuilder hotelSearchBuilder = hotelSearchBuilderConverter.toHotelSearchBuilder(params, amenities);
        List<HotelEntity> hotelEntities = hotelRepository.findHotel(hotelSearchBuilder);

        List<UUID> hotelIds = new ArrayList<>();
        for (HotelEntity entity : hotelEntities) {
            hotelIds.add(entity.getId());
        }

        Map<UUID, Integer> remainingRoomsMap = new HashMap<>();
        Map<UUID, List<String>> amenitiesMap = new HashMap<>();
        if (!hotelIds.isEmpty()) {
            List<Object[]> counts;
            if (hotelSearchBuilder.getCheckIn() != null && hotelSearchBuilder.getCheckOut() != null) {
                counts = roomRepository.countAvailableRoomsByHotelIdsAndDates(
                    hotelIds, hotelSearchBuilder.getCheckIn(), hotelSearchBuilder.getCheckOut()
                );
            } else {
                counts = roomRepository.countAvailableRoomsByHotelIds(hotelIds);
            }
            for (Object[] row : counts) {
                UUID hotelId = (UUID) row[0];
                Long count = (Long) row[1];
                remainingRoomsMap.put(hotelId, count.intValue());
            }

            List<Object[]> amenityRows = hotelRepository.findAmenityNamesByHotelIds(hotelIds);
            for (Object[] row : amenityRows) {
                UUID hotelId = (UUID) row[0];
                String amenityName = (String) row[1];
                amenitiesMap.computeIfAbsent(hotelId, k -> new ArrayList<>()).add(amenityName);
            }
        }

        List<HotelDTO> hotelDTOS = new ArrayList<HotelDTO>();
        for(HotelEntity item : hotelEntities){
            HotelDTO hotel = hotelDTOConverter.toHotelDTO(item, amenitiesMap.getOrDefault(item.getId(), new ArrayList<>()));
            hotel.setRemainingRooms(remainingRoomsMap.getOrDefault(item.getId(), 0));
            hotelDTOS.add(hotel);
        }
        return hotelDTOS;
    }

    @Override
    @Transactional(readOnly = true)
    public long countHotel(Map<String, Object> params, List<String> amenities) {
        HotelSearchBuilder hotelSearchBuilder = hotelSearchBuilderConverter.toHotelSearchBuilder(params, amenities);
        return hotelRepository.countHotel(hotelSearchBuilder);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDTO getHotelById(UUID id, Map<String, Object> params) {
        HotelEntity hotelEntity = hotelRepository.findById(id).orElse(null);
        if (hotelEntity == null || !Boolean.TRUE.equals(hotelEntity.getIsActive())) return null;

        HotelDTO hotel = hotelDTOConverter.toHotelDTO(hotelEntity);

        LocalDate checkIn = null;
        LocalDate checkOut = null;
        if (params != null) {
            checkIn = DateUtil.parseLocalDate(params.get("checkIn"));
            checkOut = DateUtil.parseLocalDate(params.get("checkOut"));
        }

        Long remaining;
        if (checkIn != null && checkOut != null) {
            remaining = roomRepository.countAvailableRoomsByHotelIdAndDates(id, checkIn, checkOut);
        } else {
            remaining = roomRepository.countAvailableRoomsByHotelId(id);
        }
        hotel.setRemainingRooms(remaining.intValue());
        return hotel;
    }
}
