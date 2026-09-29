package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.FlightBookingEntity;

import java.util.List;

public interface FlightBookingRepositoryCustom {
    /**
     * Danh sách vé cho admin, mới nhất trước.
     * @param keyword mã order, email / tên khách, tên hành khách, số hiệu chuyến (null = bỏ qua)
     */
    List<FlightBookingEntity> findForAdmin(String status, String keyword, int page, int limit);

    long countForAdmin(String status, String keyword);
}
