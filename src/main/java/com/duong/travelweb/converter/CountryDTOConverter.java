package com.duong.travelweb.converter;

import com.duong.travelweb.model.CountryDTO;
import com.duong.travelweb.repository.ContinentRepository;
import com.duong.travelweb.repository.entity.ContinentEntity;
import com.duong.travelweb.repository.entity.CountryEntity;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CountryDTOConverter {
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ContinentRepository continentRepository;

    public CountryDTO toCountryDTO(CountryEntity item){
        CountryDTO country = modelMapper.map(item,CountryDTO.class);
        ContinentEntity continentEntity = continentRepository.findNameById(item.getContinentId().longValue());
        country.setContinent(continentEntity.getName());
        return country;
    }
}
