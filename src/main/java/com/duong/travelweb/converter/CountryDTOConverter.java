package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.CountryDTO;
import com.duong.travelweb.model.entity.ContinentEntity;
import com.duong.travelweb.model.entity.CountryEntity;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CountryDTOConverter {
    @Autowired
    private ModelMapper modelMapper;

    public CountryDTO toCountryDTO(CountryEntity item){
        CountryDTO country = modelMapper.map(item,CountryDTO.class);
        if (item.getContinent() != null) {
            country.setContinent(item.getContinent().getName());
        }
        return country;
    }
}
