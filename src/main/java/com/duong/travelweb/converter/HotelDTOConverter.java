package com.duong.travelweb.converter;

import com.duong.travelweb.model.HotelDTO;
import com.duong.travelweb.repository.CityRepository;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.entity.CityEntity;
import com.duong.travelweb.repository.entity.CountryEntity;
import com.duong.travelweb.repository.entity.HotelEntity;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HotelDTOConverter {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private CountryRepository countryRepository;


    public HotelDTO toHotelDTO(HotelEntity item, Map<Long, CityEntity> cityCache ,Map<String, CountryEntity> countryCache){
        HotelDTO hotel = modelMapper.map(item,HotelDTO.class);
        Long cityId = item.getCityId().longValue();
        if (!cityCache.containsKey(cityId)) {
            cityCache.put(cityId, cityRepository.findNameById(cityId));
        }
        CityEntity cityEntity = cityCache.get(cityId);

        String countryId = cityEntity.getCountryID();
        if (countryId != null && !countryCache.containsKey(countryId)) {
            countryCache.put(countryId, countryRepository.findNameById(countryId));
        }
        CountryEntity countryEntity = countryId != null ? countryCache.get(countryId) : new CountryEntity();

        String countryName = countryEntity.getCountryName() != null ? countryEntity.getCountryName() : "";
        String cityName = cityEntity.getName() != null ? cityEntity.getName() : "";
        hotel.setAddress(item.getAddress() + " - " +cityName + " - " + countryName);
        return hotel;
    }
}
