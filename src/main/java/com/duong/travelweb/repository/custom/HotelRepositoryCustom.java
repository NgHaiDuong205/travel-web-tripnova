package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.model.entity.HotelEntity;

import java.util.List;
import java.util.UUID;

public interface HotelRepositoryCustom {
    List<HotelEntity> findHotel(HotelSearchBuilder hotelSearchBuilder);
    long countHotel(HotelSearchBuilder hotelSearchBuilder);

    /** Danh sách cho admin (kể cả khách sạn đã ẩn). active = null → tất cả. */
    List<HotelEntity> findForAdmin(String keyword, UUID destinationId, Boolean active, int page, int limit);
    long countForAdmin(String keyword, UUID destinationId, Boolean active);
}
