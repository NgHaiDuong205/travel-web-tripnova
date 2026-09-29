package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.LandmarkDTO;
import java.util.List;
import java.util.UUID;

public interface LandmarkService {
    /** Tham số null = bỏ qua; limit null = trả toàn bộ. */
    List<LandmarkDTO> findAllActiveLandmarks(String keyword, String category, Integer page, Integer limit);
    long countActiveLandmarks(String keyword, String category);
    LandmarkDTO findLandmarkByIdAndDestinationId(UUID destinationId, UUID landmarkId);
}
