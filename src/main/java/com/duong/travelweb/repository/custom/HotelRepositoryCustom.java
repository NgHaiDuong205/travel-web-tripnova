package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.model.entity.HotelEntity;

import java.util.List;
import java.util.UUID;

public interface HotelRepositoryCustom {
    List<HotelEntity> findHotel(HotelSearchBuilder hotelSearchBuilder);
    long countHotel(HotelSearchBuilder hotelSearchBuilder);

    /** Danh sách cho admin (kể cả khách sạn đã ẩn). active = null → tất cả. */
    /** managedById != null -> chỉ khách sạn do user đó quản lý (trang quản lý khách sạn). */
    List<HotelEntity> findForAdmin(String keyword, UUID destinationId, Boolean active, UUID managedById, int page, int limit);
    long countForAdmin(String keyword, UUID destinationId, Boolean active, UUID managedById);
}
