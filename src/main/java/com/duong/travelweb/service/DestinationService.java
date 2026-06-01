package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.DestinationDTO;
import com.duong.travelweb.model.dto.LandmarkDTO;

import java.util.List;
import java.util.UUID;

public interface DestinationService {
    List<DestinationDTO> findDestinations(String countryCode, String continentCode);
    DestinationDTO findById(UUID destinationId);
    List<LandmarkDTO> findLandmarksByDestinationId(UUID destinationId, String category);
}
