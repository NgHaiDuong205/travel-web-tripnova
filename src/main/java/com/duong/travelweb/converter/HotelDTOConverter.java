package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.model.entity.HotelEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class HotelDTOConverter {
    private final ModelMapper modelMapper;

    public HotelDTOConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public HotelDTO toHotelDTO(HotelEntity item){
        return toHotelDTO(item, null);
    }

    /**
     * Prefer this overload when converting a list of hotels: pass amenities pre-fetched in a
     * single batch query to avoid triggering the lazy hotelAmenities collection per hotel (N+1).
     */
    public HotelDTO toHotelDTO(HotelEntity item, List<String> amenities){
        HotelDTO hotel = modelMapper.map(item,HotelDTO.class);
        if (item.getDestination() != null && item.getDestination().getCountry() != null) {
            hotel.setCountryName(item.getDestination().getCountry().getName());
        }
        if (amenities != null) {
            hotel.setAmenities(amenities);
        } else if (item.getHotelAmenities() != null) {
            List<String> amenityList = new java.util.ArrayList<>();
            for (com.duong.travelweb.model.entity.AmenityEntity a : item.getHotelAmenities()) {
                amenityList.add(a.getName());
            }
            hotel.setAmenities(amenityList);
        }
        return hotel;
    }
}
