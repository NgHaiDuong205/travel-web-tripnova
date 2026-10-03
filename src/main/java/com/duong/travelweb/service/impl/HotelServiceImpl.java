package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.converter.HotelDTOConverter;
import com.duong.travelweb.converter.HotelSearchBuilderConverter;
import com.duong.travelweb.exception.ApiException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HotelServiceImpl implements HotelService {
    private static final double MAX_NEARBY_RADIUS_KM = 50;
    private static final int MAX_NEARBY_LIMIT = 20;

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
        return toListDTOs(hotelEntities, hotelSearchBuilder.getCheckIn(), hotelSearchBuilder.getCheckOut());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelDTO> findNearby(double lat, double lng, double radiusKm, int limit) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) throw ApiException.badRequest("Toạ độ không hợp lệ");
        double radius = Math.min(Math.max(radiusKm, 0.1), MAX_NEARBY_RADIUS_KM);
        int size = Math.min(Math.max(limit, 1), MAX_NEARBY_LIMIT);
        double dLat = radius / 111.0;
        double dLng = radius / (111.0 * Math.max(Math.cos(Math.toRadians(lat)), 0.01));

        Map<UUID, Double> distances = new LinkedHashMap<>();
        for (Object[] row : hotelRepository.findNearby(lat, lng, dLat, dLng, size)) {
            double km = ((Number) row[1]).doubleValue();
            if (km <= radius) distances.put((UUID) row[0], km);
        }
        if (distances.isEmpty()) return new ArrayList<>();

        Map<UUID, HotelEntity> byId = new HashMap<>();
        for (HotelEntity entity : hotelRepository.findAllById(distances.keySet())) byId.put(entity.getId(), entity);
        List<HotelEntity> ordered = new ArrayList<>();
        for (UUID id : distances.keySet()) {
            if (byId.containsKey(id)) ordered.add(byId.get(id));
        }
        List<HotelDTO> result = toListDTOs(ordered, null, null);
        for (HotelDTO dto : result) dto.setDistanceKm(Math.round(distances.get(dto.getId()) * 100) / 100.0);
        return result;
    }

    /** DTO danh sách: tiện nghi + số phòng còn trống (theo ngày nếu có) lấy theo lô. */
    private List<HotelDTO> toListDTOs(List<HotelEntity> hotelEntities, LocalDate checkIn, LocalDate checkOut) {
        List<UUID> hotelIds = new ArrayList<>();
        for (HotelEntity entity : hotelEntities) {
            hotelIds.add(entity.getId());
        }

        Map<UUID, Integer> remainingRoomsMap = new HashMap<>();
        Map<UUID, List<String>> amenitiesMap = new HashMap<>();
        if (!hotelIds.isEmpty()) {
            List<Object[]> counts;
            if (checkIn != null && checkOut != null) {
                counts = roomRepository.countAvailableRoomsByHotelIdsAndDates(hotelIds, checkIn, checkOut);
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
        if (hotelEntity == null || !Boolean.TRUE.equals(hotelEntity.getIsActive())) throw ApiException.notFound("Không tìm thấy khách sạn");

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
