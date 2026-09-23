package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.AmenityDTO;
import com.duong.travelweb.model.entity.AmenityEntity;
import com.duong.travelweb.repository.AmenityRepository;
import com.duong.travelweb.service.AmenityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AmenityServiceImpl implements AmenityService {
    private final AmenityRepository amenityRepository;

    public AmenityServiceImpl(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityDTO> findAmenities(String category) {
        List<AmenityEntity> amenities = category == null || category.isBlank()
                ? amenityRepository.findAllOrdered()
                : amenityRepository.findByCategory(category.trim());
        return amenities.stream().map(this::toDTO).toList();
    }

    private AmenityDTO toDTO(AmenityEntity entity) {
        AmenityDTO dto = new AmenityDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setIconUrl(entity.getIconUrl());
        dto.setCategory(entity.getCategory());
        return dto;
    }
}
