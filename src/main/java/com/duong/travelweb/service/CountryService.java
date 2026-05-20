package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.CountryDTO;


import com.duong.travelweb.model.dto.CountryRequestDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CountryService {
    List<CountryDTO> findCountry(Map<String,Object> params, List<String> typeCode);
    CountryDTO getCountryById(UUID id);
    void createCountry(CountryRequestDTO countryRequestDTO);
    void deleteCountry(UUID id);
}
