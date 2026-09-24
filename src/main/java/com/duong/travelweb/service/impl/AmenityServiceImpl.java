package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AdminAmenityDTO;
import com.duong.travelweb.model.dto.AmenityDTO;
import com.duong.travelweb.model.dto.AmenityRequestDTO;
import com.duong.travelweb.model.entity.AmenityEntity;
import com.duong.travelweb.repository.AmenityRepository;
import com.duong.travelweb.service.AmenityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AmenityServiceImpl implements AmenityService {
    private final AmenityRepository amenityRepository;

    public AmenityServiceImpl(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityDTO> findAmenities(String category) {
        return findEntities(category).stream().map(e -> fill(new AmenityDTO(), e)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAmenityDTO> findForAdmin(String category) {
        Map<UUID, Long> hotelCounts = toCountMap(amenityRepository.countHotelsByAmenity());
        Map<UUID, Long> roomTypeCounts = toCountMap(amenityRepository.countRoomTypesByAmenity());
        return findEntities(category).stream()
                .map(e -> toAdminDTO(e, hotelCounts.getOrDefault(e.getId(), 0L), roomTypeCounts.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public AdminAmenityDTO create(AmenityRequestDTO request) {
        AmenityEntity amenity = new AmenityEntity();
        apply(amenity, request, null);
        return toAdminDTO(amenityRepository.save(amenity), 0, 0);
    }

    @Override
    @Transactional
    public AdminAmenityDTO update(UUID amenityId, AmenityRequestDTO request) {
        AmenityEntity amenity = findAmenity(amenityId);
        apply(amenity, request, amenityId);
        amenity = amenityRepository.save(amenity);
        Map<UUID, Long> hotelCounts = toCountMap(amenityRepository.countHotelsByAmenity());
        Map<UUID, Long> roomTypeCounts = toCountMap(amenityRepository.countRoomTypesByAmenity());
        return toAdminDTO(amenity, hotelCounts.getOrDefault(amenityId, 0L), roomTypeCounts.getOrDefault(amenityId, 0L));
    }

    @Override
    @Transactional
    public void delete(UUID amenityId, boolean force) {
        AmenityEntity amenity = findAmenity(amenityId);
        if (!force) {
            long hotels = toCountMap(amenityRepository.countHotelsByAmenity()).getOrDefault(amenityId, 0L);
            long roomTypes = toCountMap(amenityRepository.countRoomTypesByAmenity()).getOrDefault(amenityId, 0L);
            if (hotels > 0 || roomTypes > 0) {
                throw ApiException.conflict("Tiện ích đang được dùng bởi " + hotels + " khách sạn và "
                        + roomTypes + " hạng phòng. Xoá sẽ gỡ khỏi tất cả.");
            }
        }
        amenityRepository.delete(amenity);
    }

    private void apply(AmenityEntity amenity, AmenityRequestDTO request, UUID excludeId) {
        String name = request.getName().trim();
        String category = request.getCategory().trim();
        if (amenityRepository.existsByNameInCategory(name, category, excludeId)) {
            throw ApiException.conflict("Tiện ích \"" + name + "\" đã có trong nhóm " + category);
        }
        amenity.setName(name);
        amenity.setCategory(category);
        String iconUrl = request.getIconUrl();
        amenity.setIconUrl(iconUrl == null || iconUrl.isBlank() ? null : iconUrl.trim());
    }

    private List<AmenityEntity> findEntities(String category) {
        return category == null || category.isBlank()
                ? amenityRepository.findAllOrdered()
                : amenityRepository.findByCategory(category.trim());
    }

    private AmenityEntity findAmenity(UUID amenityId) {
        return amenityRepository.findById(amenityId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tiện ích"));
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private AdminAmenityDTO toAdminDTO(AmenityEntity entity, long hotelCount, long roomTypeCount) {
        AdminAmenityDTO dto = fill(new AdminAmenityDTO(), entity);
        dto.setHotelCount(hotelCount);
        dto.setRoomTypeCount(roomTypeCount);
        return dto;
    }

    private <T extends AmenityDTO> T fill(T dto, AmenityEntity entity) {
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setIconUrl(entity.getIconUrl());
        dto.setCategory(entity.getCategory());
        return dto;
    }
}
