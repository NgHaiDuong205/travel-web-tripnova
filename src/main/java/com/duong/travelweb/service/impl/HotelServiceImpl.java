package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.HotelDTOConverter;
import com.duong.travelweb.model.HotelDTO;
import com.duong.travelweb.repository.CityRepository;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.entity.CityEntity;
import com.duong.travelweb.repository.entity.CountryEntity;
import com.duong.travelweb.repository.entity.HotelEntity;
import com.duong.travelweb.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HotelServiceImpl implements HotelService {
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private HotelDTOConverter hotelDTOConverter;

    @Override
    public List<HotelDTO> findHotel(Map<String, Object> params, List<String> typeCode) {
        List<HotelEntity> hotelEntities = hotelRepository.findHotel(params,typeCode);
        List<HotelDTO> hotelDTOS = new ArrayList<HotelDTO>();
        
        java.util.Map<Long, CityEntity> cityCache = new java.util.HashMap<>();
        java.util.Map<String, CountryEntity> countryCache = new java.util.HashMap<>();
        
        for(HotelEntity item : hotelEntities){
            HotelDTO hotel = hotelDTOConverter.toHotelDTO(item,cityCache,countryCache);
            hotelDTOS.add(hotel);
        }
        return hotelDTOS;
    }
}
