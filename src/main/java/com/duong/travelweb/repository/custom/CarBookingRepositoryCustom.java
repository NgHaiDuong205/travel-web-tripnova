package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.CarBookingEntity;

import java.util.List;

public interface CarBookingRepositoryCustom {
    /**
     * Danh sách cho admin, mới nhất trước, kèm xe / order / user.
     * @param status  trạng thái booking (null = tất cả)
     * @param keyword mã order, email / tên khách, tên xe, biển số (null = bỏ qua)
     */
    List<CarBookingEntity> findForAdmin(String status, String keyword, int page, int limit);

    long countForAdmin(String status, String keyword);
}
