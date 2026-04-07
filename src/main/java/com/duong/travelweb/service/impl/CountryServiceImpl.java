package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.CountryDTO;
import com.duong.travelweb.repository.ContinentRepository;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.entity.ContinentEntity;
import com.duong.travelweb.repository.entity.CountryEntity;
import com.duong.travelweb.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CountryServiceImpl implements CountryService {
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private ContinentRepository continentRepository;
    @Override
    public List<CountryDTO> findCountry(Map<String,Object> params,List<String> typeCode) {
        List<CountryEntity> countryEntities = countryRepository.findCountry(params,typeCode);
        List<CountryDTO> result = new ArrayList<CountryDTO>();
        for(CountryEntity item : countryEntities){
            CountryDTO country = new CountryDTO();
            country.setId(item.getId());
            country.setCountryName(item.getCountryName());
            country.setFlag(item.getFlag());
            ContinentEntity continentEntity = continentRepository.findNameById(item.getContinentId().longValue());
            country.setContinent(continentEntity.getName());
            result.add(country);
        }
        return result;
    }
}
