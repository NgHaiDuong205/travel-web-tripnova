package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.LandmarkDTO;
import com.duong.travelweb.model.entity.LandmarkEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class LandmarkDTOConverter {
    private final ModelMapper modelMapper;

    public LandmarkDTOConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public LandmarkDTO toLandmarkDTO(LandmarkEntity item) {
        LandmarkDTO dto = modelMapper.map(item, LandmarkDTO.class);
        if (item.getDestination() != null) {
            dto.setDestinationId(item.getDestination().getId());
        }
        return dto;
    }
}
