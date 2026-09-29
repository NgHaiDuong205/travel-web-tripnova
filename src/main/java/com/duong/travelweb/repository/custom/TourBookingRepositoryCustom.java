package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.TourBookingEntity;

import java.util.List;

public interface TourBookingRepositoryCustom {
    /**
     * Danh sách đơn tour cho admin, mới nhất trước.
     * @param keyword mã order, email / tên khách, tên người liên hệ, tên tour (null = bỏ qua)
     */
    List<TourBookingEntity> findForAdmin(String status, String keyword, int page, int limit);

    long countForAdmin(String status, String keyword);
}
