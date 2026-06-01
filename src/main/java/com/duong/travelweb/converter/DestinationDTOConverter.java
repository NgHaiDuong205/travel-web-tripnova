package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.DestinationDTO;
import com.duong.travelweb.model.entity.DestinationEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class DestinationDTOConverter {
    private final ModelMapper modelMapper;

    public DestinationDTOConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public DestinationDTO toDestinationDTO(DestinationEntity item) {
        DestinationDTO dto = modelMapper.map(item, DestinationDTO.class);
        if (item.getCountry() != null) {
            dto.setCountryName(item.getCountry().getName());
            dto.setCountryCode(item.getCountry().getCountryCode());
            if (item.getCountry().getContinent() != null) {
                dto.setContinentName(item.getCountry().getContinent().getName());
                dto.setContinentCode(item.getCountry().getContinent().getCode());
            }
        }
        return dto;
    }
}
