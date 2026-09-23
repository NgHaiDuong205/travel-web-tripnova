package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.HotelBookingEntity;

import java.util.List;
import java.util.UUID;

public interface HotelBookingRepositoryCustom {
    /**
     * @param statusGroup all | upcoming | completed | cancelled | pending (null = all)
     * @param page        bắt đầu từ 1
     */
    List<HotelBookingEntity> findByUser(UUID userId, String statusGroup, int page, int limit);

    long countByUser(UUID userId, String statusGroup);
}
