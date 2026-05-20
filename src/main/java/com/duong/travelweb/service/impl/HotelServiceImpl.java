package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.converter.HotelDTOConverter;
import com.duong.travelweb.converter.HotelSearchBuiderConverter;
import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HotelServiceImpl implements HotelService {
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private HotelDTOConverter hotelDTOConverter;
    @Autowired
    private HotelSearchBuiderConverter hotelSearchBuiderConverter;
    @Override
    public List<HotelDTO> findHotel(Map<String, Object> params, List<String> typeCode) {
        HotelSearchBuilder hotelSearchBuider = hotelSearchBuiderConverter.toHotelSearchBuider(params,typeCode);
        List<HotelEntity> hotelEntities = hotelRepository.findHotel(hotelSearchBuider);
        List<HotelDTO> hotelDTOS = new ArrayList<HotelDTO>();
        
        for(HotelEntity item : hotelEntities){
            HotelDTO hotel = hotelDTOConverter.toHotelDTO(item);
            hotelDTOS.add(hotel);
        }
        return hotelDTOS;
    }
}
