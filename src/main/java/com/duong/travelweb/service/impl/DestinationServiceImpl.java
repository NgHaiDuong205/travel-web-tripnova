package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.DestinationDTOConverter;
import com.duong.travelweb.converter.LandmarkDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.DestinationDTO;
import com.duong.travelweb.model.dto.LandmarkDTO;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.model.entity.LandmarkEntity;
import com.duong.travelweb.repository.DestinationRepository;
import com.duong.travelweb.repository.LandmarkRepository;
import com.duong.travelweb.service.DestinationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DestinationServiceImpl implements DestinationService {

    private final DestinationRepository destinationRepository;
    private final DestinationDTOConverter destinationDTOConverter;
    private final LandmarkRepository landmarkRepository;
    private final LandmarkDTOConverter landmarkDTOConverter;

    public DestinationServiceImpl(DestinationRepository destinationRepository, 
                                  DestinationDTOConverter destinationDTOConverter,
                                  LandmarkRepository landmarkRepository,
                                  LandmarkDTOConverter landmarkDTOConverter) {
        this.destinationRepository = destinationRepository;
        this.destinationDTOConverter = destinationDTOConverter;
        this.landmarkRepository = landmarkRepository;
        this.landmarkDTOConverter = landmarkDTOConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationDTO> findDestinations(String keyword, String countryCode, String continentCode, Boolean popular,
                                                 Integer page, Integer limit) {
        List<DestinationEntity> entities = destinationRepository.findPublic(blankToNull(keyword), blankToNull(countryCode),
                blankToNull(continentCode), popular, page, limit);
        List<DestinationDTO> result = new ArrayList<>();
        for (DestinationEntity item : entities) {
            result.add(destinationDTOConverter.toDestinationDTO(item));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public DestinationDTO findById(UUID destinationId) {
        DestinationEntity entity = destinationRepository.findById(destinationId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy điểm đến"));
        if (Boolean.FALSE.equals(entity.getIsActive())) throw ApiException.notFound("Không tìm thấy điểm đến");
        return destinationDTOConverter.toDestinationDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandmarkDTO> findLandmarksByDestinationId(UUID destinationId, String category) {
        DestinationEntity destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy điểm đến"));
        if (Boolean.FALSE.equals(destination.getIsActive())) {
            throw ApiException.notFound("Không tìm thấy điểm đến");
        }
        List<LandmarkEntity> landmarks = landmarkRepository.findActiveLandmarksByDestinationId(destinationId, category);
        List<LandmarkDTO> result = new ArrayList<>();
        for (LandmarkEntity item : landmarks) {
            result.add(landmarkDTOConverter.toLandmarkDTO(item));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countDestinations(String keyword, String countryCode, String continentCode, Boolean popular) {
        return destinationRepository.countPublic(blankToNull(keyword), blankToNull(countryCode), blankToNull(continentCode), popular);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
