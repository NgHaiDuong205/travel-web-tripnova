package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.LandmarkDTO;
import java.util.List;
import java.util.UUID;

public interface LandmarkService {
    List<LandmarkDTO> findAllActiveLandmarks(String category);
    LandmarkDTO findLandmarkByIdAndDestinationId(UUID destinationId, UUID landmarkId);
}
