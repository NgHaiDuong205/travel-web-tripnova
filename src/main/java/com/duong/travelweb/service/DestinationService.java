package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.DestinationDTO;
import com.duong.travelweb.model.dto.LandmarkDTO;

import java.util.List;
import java.util.UUID;

public interface DestinationService {
    /** Tham số null = bỏ qua; limit null = trả toàn bộ (FE cũ gọi không phân trang). */
    List<DestinationDTO> findDestinations(String keyword, String countryCode, String continentCode, Boolean popular,
                                          Integer page, Integer limit);
    long countDestinations(String keyword, String countryCode, String continentCode, Boolean popular);
    DestinationDTO findById(UUID destinationId);
    List<LandmarkDTO> findLandmarksByDestinationId(UUID destinationId, String category);
}
