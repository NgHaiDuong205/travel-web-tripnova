package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.model.entity.HotelEntity;

import java.util.List;
import java.util.UUID;

public interface HotelRepositoryCustom {
    List<HotelEntity> findHotel(HotelSearchBuilder hotelSearchBuider);
    HotelEntity findNameById(UUID id);
}
