package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.CountrySearchBuilder;
import com.duong.travelweb.converter.CountryDTOConverter;
import com.duong.travelweb.converter.CountrySearchBuilderConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.CountryDTO;
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
    private final CountrySearchBuilderConverter countrySearchBuilderConverter;
    private final CountryRepository countryRepository;

    public CountryServiceImpl(CountryDTOConverter countryDTOConverter, 
                              CountrySearchBuilderConverter countrySearchBuilderConverter, 
                              CountryRepository countryRepository) {
        this.countryDTOConverter = countryDTOConverter;
        this.countrySearchBuilderConverter = countrySearchBuilderConverter;
        this.countryRepository = countryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CountryDTO> findCountry(Map<String,Object> params,List<String> typeCode) {
        CountrySearchBuilder countrySearchBuilder = countrySearchBuilderConverter.toCountrySearchBuilder(params,typeCode);
        List<CountryEntity> countryEntities = countryRepository.findCountry(countrySearchBuilder);
        List<CountryDTO> result = new ArrayList<CountryDTO>();
        for(CountryEntity item : countryEntities){
            CountryDTO country = countryDTOConverter.toCountryDTO(item);
            result.add(country);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CountryDTO getCountryById(UUID id) {
        CountryEntity countryEntity = countryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy quốc gia"));
        return countryDTOConverter.toCountryDTO(countryEntity);
    }
}
