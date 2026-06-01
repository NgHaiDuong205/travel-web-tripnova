package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.converter.HotelDTOConverter;
import com.duong.travelweb.converter.HotelSearchBuilderConverter;
import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.service.HotelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HotelServiceImpl implements HotelService {
    private final HotelRepository hotelRepository;
    private final HotelDTOConverter hotelDTOConverter;
    private final HotelSearchBuilderConverter hotelSearchBuilderConverter;

    public HotelServiceImpl(HotelRepository hotelRepository, HotelDTOConverter hotelDTOConverter, HotelSearchBuilderConverter hotelSearchBuilderConverter) {
        this.hotelRepository = hotelRepository;
        this.hotelDTOConverter = hotelDTOConverter;
        this.hotelSearchBuilderConverter = hotelSearchBuilderConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelDTO> findHotel(Map<String, Object> params, List<String> amenities) {
        HotelSearchBuilder hotelSearchBuilder = hotelSearchBuilderConverter.toHotelSearchBuilder(params, amenities);
        List<HotelEntity> hotelEntities = hotelRepository.findHotel(hotelSearchBuilder);
        List<HotelDTO> hotelDTOS = new ArrayList<HotelDTO>();
        
        for(HotelEntity item : hotelEntities){
            HotelDTO hotel = hotelDTOConverter.toHotelDTO(item);
            hotelDTOS.add(hotel);
        }
        return hotelDTOS;
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDTO getHotelById(UUID id) {
        HotelEntity hotelEntity = hotelRepository.findById(id).orElse(null);
        if (hotelEntity == null) return null;
        return hotelDTOConverter.toHotelDTO(hotelEntity);
    }
}
