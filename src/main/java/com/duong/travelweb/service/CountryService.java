package com.duong.travelweb.service;

import com.duong.travelweb.model.CountryDTO;


import java.util.List;

public interface CountryService {
    List<CountryDTO> findAll(String countryName);
}
