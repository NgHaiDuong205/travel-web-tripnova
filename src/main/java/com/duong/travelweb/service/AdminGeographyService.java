package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AdminContinentDTO;
import com.duong.travelweb.model.dto.AdminCountryDTO;
import com.duong.travelweb.model.dto.AdminDestinationDTO;
import com.duong.travelweb.model.dto.AdminLandmarkDTO;
import com.duong.travelweb.model.dto.ContinentRequestDTO;
import com.duong.travelweb.model.dto.CountryRequestDTO;
import com.duong.travelweb.model.dto.DestinationRequestDTO;
import com.duong.travelweb.model.dto.LandmarkRequestDTO;

import java.util.List;
import java.util.UUID;

/** Quản trị châu lục / quốc gia / điểm đến / địa danh. */
public interface AdminGeographyService {
    List<AdminContinentDTO> findContinents();
    AdminContinentDTO createContinent(ContinentRequestDTO request);
    AdminContinentDTO updateContinent(UUID continentId, ContinentRequestDTO request);
    /** Xoá cứng; 409 nếu còn quốc gia. */
    void deleteContinent(UUID continentId);

    List<AdminCountryDTO> findCountries(String keyword, UUID continentId);
    AdminCountryDTO createCountry(CountryRequestDTO request);
    AdminCountryDTO updateCountry(UUID countryId, CountryRequestDTO request);
    /** Xoá cứng; 409 nếu còn điểm đến. */
    void deleteCountry(UUID countryId);

    List<AdminDestinationDTO> findDestinations(String keyword, UUID countryId, Boolean active, int page, int limit);
    long countDestinations(String keyword, UUID countryId, Boolean active);
    AdminDestinationDTO getDestination(UUID destinationId);
    AdminDestinationDTO createDestination(DestinationRequestDTO request);
    AdminDestinationDTO updateDestination(UUID destinationId, DestinationRequestDTO request);
    /** Xoá mềm (is_active = false) vì khách sạn tham chiếu với ON DELETE RESTRICT. */
    void deactivateDestination(UUID destinationId);

    List<AdminLandmarkDTO> findLandmarks(String keyword, UUID destinationId, String category, Boolean active, int page, int limit);
    long countLandmarks(String keyword, UUID destinationId, String category, Boolean active);
    AdminLandmarkDTO createLandmark(LandmarkRequestDTO request);
    AdminLandmarkDTO updateLandmark(UUID landmarkId, LandmarkRequestDTO request);
    void deactivateLandmark(UUID landmarkId);
}
