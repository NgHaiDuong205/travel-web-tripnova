package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.LandmarkDTOConverter;
import com.duong.travelweb.model.dto.LandmarkDTO;
import com.duong.travelweb.model.entity.LandmarkEntity;
import com.duong.travelweb.repository.LandmarkRepository;
import com.duong.travelweb.service.LandmarkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LandmarkServiceImpl implements LandmarkService {

    private final LandmarkRepository landmarkRepository;
    private final LandmarkDTOConverter landmarkDTOConverter;

    public LandmarkServiceImpl(LandmarkRepository landmarkRepository, 
                               LandmarkDTOConverter landmarkDTOConverter) {
        this.landmarkRepository = landmarkRepository;
        this.landmarkDTOConverter = landmarkDTOConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandmarkDTO> findAllActiveLandmarks(String category) {
        List<LandmarkEntity> entities = landmarkRepository.findAllActiveLandmarks(category);
        List<LandmarkDTO> result = new ArrayList<>();
        for (LandmarkEntity item : entities) {
            result.add(landmarkDTOConverter.toLandmarkDTO(item));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public LandmarkDTO findLandmarkByIdAndDestinationId(UUID destinationId, UUID landmarkId) {
        LandmarkEntity landmarkEntity = landmarkRepository.findActiveLandmarkByIdAndDestinationId(landmarkId, destinationId).orElse(null);
        if (landmarkEntity != null) {
            return landmarkDTOConverter.toLandmarkDTO(landmarkEntity);
        }
        return null;
    }
}
