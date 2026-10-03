package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.DestinationDTO;
import com.duong.travelweb.model.dto.LandmarkDTO;

import java.util.List;
import java.util.UUID;

public interface DestinationService {
    /** Tham số null = bỏ qua; limit null = trả toàn bộ (FE cũ gọi không phân trang). */
    /** category: coastal | historical | mountain | urban | hidden (null = mọi nhóm; giá trị lạ → 400). */
    List<DestinationDTO> findDestinations(String keyword, String countryCode, String continentCode, Boolean popular,
                                          String category, Integer page, Integer limit);
    long countDestinations(String keyword, String countryCode, String continentCode, Boolean popular, String category);
    DestinationDTO findById(UUID destinationId);
    List<LandmarkDTO> findLandmarksByDestinationId(UUID destinationId, String category);
}
