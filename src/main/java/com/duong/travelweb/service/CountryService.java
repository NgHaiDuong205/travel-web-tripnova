package com.duong.travelweb.service;

import com.duong.travelweb.model.CountryDTO;


import java.util.List;
import java.util.Map;

public interface CountryService {
    List<CountryDTO> findCountry(Map<String,Object> params, List<String> typeCode);
}
