package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.model.entity.HotelEntity;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HotelDTOConverter {
    @Autowired
    private ModelMapper modelMapper;

    public HotelDTO toHotelDTO(HotelEntity item){
        HotelDTO hotel = modelMapper.map(item,HotelDTO.class);
        return hotel;
    }
}
