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
    public List<CountryDTO> findAll(String countryName) {
        List<CountryEntity> countryEntities = countryRepository.findAll(countryName);
        List<CountryDTO> result = new ArrayList<CountryDTO>();
        for(CountryEntity item : countryEntities){
            CountryDTO country = new CountryDTO();
            country.setCountry_name(item.getCountry_name());
            country.setCode(item.getCode());
            country.setCapital(item.getCapital());
            country.setPopulation(item.getPopulation());
            country.setFlag(item.getFlag());
            country.setCapital(item.getCapital());
            country.setCountry_id(item.getCountry_id());
            country.setCurrencies(item.getCurrencies());
            country.setRegion(item.getRegion());
            result.add(country);
        }
        return result;
    }
}
