package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.model.entity.HotelEntity;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class HotelDTOConverter {
    @Autowired
    private ModelMapper modelMapper;

    public HotelDTO toHotelDTO(HotelEntity item){
        HotelDTO hotel = modelMapper.map(item,HotelDTO.class);
        if (item.getDestination() != null && item.getDestination().getCountry() != null) {
            hotel.setCountryName(item.getDestination().getCountry().getName());
        }
        if (item.getHotelAmenities() != null) {
            List<String> amenityList = new java.util.ArrayList<>();
            for (com.duong.travelweb.model.entity.AmenityEntity a : item.getHotelAmenities()) {
                amenityList.add(a.getName());
            }
            hotel.setAmenities(amenityList);
        }
        return hotel;
    }
}
