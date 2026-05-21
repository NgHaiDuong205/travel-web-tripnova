package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.CountrySearchBuilder;
import com.duong.travelweb.converter.CountryDTOConverter;
import com.duong.travelweb.converter.CountrySearchBuiderConverter;
import com.duong.travelweb.model.dto.CountryDTO;
import com.duong.travelweb.model.dto.CountryRequestDTO;
import com.duong.travelweb.model.entity.ContinentEntity;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.model.entity.CountryEntity;
import com.duong.travelweb.service.CountryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CountryServiceImpl implements CountryService {
    
    private final CountryDTOConverter countryDTOConverter;
    private final CountrySearchBuiderConverter countrySearchBuiderConverter;
    private final CountryRepository countryRepository;

    public CountryServiceImpl(CountryDTOConverter countryDTOConverter, 
                              CountrySearchBuiderConverter countrySearchBuiderConverter, 
                              CountryRepository countryRepository) {
        this.countryDTOConverter = countryDTOConverter;
        this.countrySearchBuiderConverter = countrySearchBuiderConverter;
        this.countryRepository = countryRepository;
    }

    @Override
    public List<CountryDTO> findCountry(Map<String,Object> params,List<String> typeCode) {
        CountrySearchBuilder countrySearchBuilder = countrySearchBuiderConverter.toCountrySearchBuilder(params,typeCode);
        List<CountryEntity> countryEntities = countryRepository.findCountry(countrySearchBuilder);
        List<CountryDTO> result = new ArrayList<CountryDTO>();
        for(CountryEntity item : countryEntities){
            CountryDTO country = countryDTOConverter.toCountryDTO(item);
            result.add(country);
        }
        return result;
    }

    @Override
    public CountryDTO getCountryById(UUID id) {
        CountryEntity countryEntity = countryRepository.findById(id).orElse(null);
        if(countryEntity == null) return new CountryDTO();
        return countryDTOConverter.toCountryDTO(countryEntity);
    }

    @Override
    @Transactional
    public void createCountry(CountryRequestDTO countryRequestDTO) {
        CountryEntity countryEntity = new CountryEntity();
        countryEntity.setId(countryRequestDTO.getId());
        countryEntity.setName(countryRequestDTO.getName());
        countryEntity.setCountryCode(countryRequestDTO.getCountryCode());
        countryEntity.setSlug(countryRequestDTO.getSlug());
        countryEntity.setImageUrl(countryRequestDTO.getImageUrl());
        countryEntity.setDescription(countryRequestDTO.getDescription());
        
        ContinentEntity continentEntity = new ContinentEntity();
        continentEntity.setId(countryRequestDTO.getContinentId());
        countryEntity.setContinent(continentEntity);
        
        countryRepository.save(countryEntity);
    }

    @Override
    @Transactional
    public void deleteCountry(UUID id) {
        countryRepository.deleteById(id);
    }
}
