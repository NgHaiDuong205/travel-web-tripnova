package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.CountryDTO;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.entity.CountryEntity;
import com.duong.travelweb.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class CountryServiceImpl implements CountryService {
    @Autowired
    private CountryRepository countryRepository;
    @Override
    public List<CountryDTO> findCountry(String countryName) {
        List<CountryEntity> countryEntities = countryRepository.findCountry(countryName);
        List<CountryDTO> result = new ArrayList<CountryDTO>();
        for(CountryEntity item : countryEntities){
            CountryDTO country = new CountryDTO();
            country.setId(item.getId());
            country.setCountryName(item.getCountryName());
            country.setFlag(item.getFlag());
            country.setContinent(item.getContinent());
            result.add(country);
        }
        return result;
    }
}
