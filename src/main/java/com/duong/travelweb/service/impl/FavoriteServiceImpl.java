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
import com.duong.travelweb.service.CarService;
import com.duong.travelweb.service.EntityReferenceService;
import com.duong.travelweb.service.FavoriteService;
import com.duong.travelweb.service.FlightService;
import com.duong.travelweb.service.TourService;
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
    private static final Set<String> SUPPORTED_TYPES = Set.of("hotel", "tour", "car", "flight");

    private final FavoriteRepository favoriteRepository;
    private final HotelRepository hotelRepository;
    private final HotelDTOConverter hotelDTOConverter;
    private final EntityReferenceService entityReferenceService;
    private final TourService tourService;
    private final CarService carService;
    private final FlightService flightService;

    public FavoriteServiceImpl(FavoriteRepository favoriteRepository,
                               HotelRepository hotelRepository,
                               HotelDTOConverter hotelDTOConverter,
                               EntityReferenceService entityReferenceService,
                               TourService tourService,
                               CarService carService,
                               FlightService flightService) {
        this.favoriteRepository = favoriteRepository;
        this.hotelRepository = hotelRepository;
        this.hotelDTOConverter = hotelDTOConverter;
        this.entityReferenceService = entityReferenceService;
        this.tourService = tourService;
        this.carService = carService;
        this.flightService = flightService;
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
        if ("hotel".equals(request.getItemType())) {
            hotelRepository.findById(request.getItemId())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách sạn"));
        } else {
            // tour / xe / chuyến bay phải còn hoạt động
            entityReferenceService.requireActiveName(request.getItemType(), request.getItemId());
        }

        FavoriteEntity favorite = favoriteRepository.findOne(userId, request.getItemType(), request.getItemId())
                .orElseGet(() -> {
                    FavoriteEntity entity = new FavoriteEntity();
                    entity.setUserId(userId);
                    entity.setItemType(request.getItemType());
                    entity.setItemId(request.getItemId());
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

    /** Đối tượng đã bị xoá hẳn -> null (FE hiện "không còn tồn tại"). */
    private static <T> T orNull(java.util.function.Supplier<T> loader) {
        try {
            return loader.get();
        } catch (ApiException e) {
            return null;
        }
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
            switch (f.getItemType()) {
                case "hotel" -> dto.setHotel(hotels.get(f.getItemId()));
                case "tour" -> dto.setTour(orNull(() -> tourService.get(f.getItemId())));
                case "car" -> dto.setCar(orNull(() -> carService.get(f.getItemId())));
                case "flight" -> dto.setFlight(orNull(() -> flightService.get(f.getItemId())));
                default -> {
                }
            }
            return dto;
        }).toList();
    }
}
