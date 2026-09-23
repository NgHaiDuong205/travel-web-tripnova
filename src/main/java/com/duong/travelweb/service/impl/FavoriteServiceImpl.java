package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.HotelDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.FavoriteCheckDTO;
import com.duong.travelweb.model.dto.FavoriteDTO;
import com.duong.travelweb.model.dto.FavoriteRequestDTO;
import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.model.entity.FavoriteEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.repository.FavoriteRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FavoriteServiceImpl implements FavoriteService {
    /** Các loại đã có dữ liệu trong hệ thống; tour/car/flight mở khi có entity tương ứng. */
    private static final Set<String> SUPPORTED_TYPES = Set.of("hotel");

    private final FavoriteRepository favoriteRepository;
    private final HotelRepository hotelRepository;
    private final HotelDTOConverter hotelDTOConverter;

    public FavoriteServiceImpl(FavoriteRepository favoriteRepository,
                               HotelRepository hotelRepository,
                               HotelDTOConverter hotelDTOConverter) {
        this.favoriteRepository = favoriteRepository;
        this.hotelRepository = hotelRepository;
        this.hotelDTOConverter = hotelDTOConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteDTO> findMyFavorites(UUID userId, String itemType) {
        List<FavoriteEntity> favorites = itemType == null || itemType.isBlank() || "all".equals(itemType)
                ? favoriteRepository.findByUserId(userId)
                : favoriteRepository.findByUserIdAndItemType(userId, itemType);
        return toDTOs(favorites);
    }

    @Override
    @Transactional
    public FavoriteDTO addFavorite(UUID userId, FavoriteRequestDTO request) {
        if (!SUPPORTED_TYPES.contains(request.getItemType())) {
            throw ApiException.badRequest("Chưa hỗ trợ lưu yêu thích cho loại " + request.getItemType());
        }
        HotelEntity hotel = hotelRepository.findById(request.getItemId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách sạn"));

        FavoriteEntity favorite = favoriteRepository.findOne(userId, request.getItemType(), request.getItemId())
                .orElseGet(() -> {
                    FavoriteEntity entity = new FavoriteEntity();
                    entity.setUserId(userId);
                    entity.setItemType(request.getItemType());
                    entity.setItemId(hotel.getId());
                    entity.setCreatedAt(LocalDateTime.now());
                    return favoriteRepository.save(entity);
                });
        return toDTOs(List.of(favorite)).get(0);
    }

    @Override
    @Transactional
    public void removeFavorite(UUID userId, UUID favoriteId) {
        FavoriteEntity favorite = favoriteRepository.findById(favoriteId)
                .filter(f -> f.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy mục yêu thích"));
        favoriteRepository.delete(favorite);
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteCheckDTO check(UUID userId, String itemType, UUID itemId) {
        return favoriteRepository.findOne(userId, itemType, itemId)
                .map(f -> new FavoriteCheckDTO(true, f.getId()))
                .orElseGet(() -> new FavoriteCheckDTO(false, null));
    }

    /** Ghép chi tiết khách sạn theo lô (1 query hotels + 1 query amenities). */
    private List<FavoriteDTO> toDTOs(List<FavoriteEntity> favorites) {
        List<UUID> hotelIds = favorites.stream()
                .filter(f -> "hotel".equals(f.getItemType()))
                .map(FavoriteEntity::getItemId)
                .toList();
        Map<UUID, HotelDTO> hotels = new HashMap<>();
        if (!hotelIds.isEmpty()) {
            Map<UUID, List<String>> amenities = new HashMap<>();
            for (Object[] row : hotelRepository.findAmenityNamesByHotelIds(hotelIds)) {
                amenities.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add((String) row[1]);
            }
            for (HotelEntity hotel : hotelRepository.findAllById(hotelIds)) {
                hotels.put(hotel.getId(), hotelDTOConverter.toHotelDTO(hotel,
                        amenities.getOrDefault(hotel.getId(), new ArrayList<>())));
            }
        }
        return favorites.stream().map(f -> {
            FavoriteDTO dto = new FavoriteDTO();
            dto.setId(f.getId());
            dto.setItemType(f.getItemType());
            dto.setItemId(f.getItemId());
            dto.setCreatedAt(f.getCreatedAt());
            dto.setHotel(hotels.get(f.getItemId()));
            return dto;
        }).toList();
    }
}
