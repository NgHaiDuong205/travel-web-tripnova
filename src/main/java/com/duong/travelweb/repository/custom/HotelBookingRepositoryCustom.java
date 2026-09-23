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

    /**
     * Danh sách cho admin.
     * @param status trạng thái booking chính xác (null = tất cả)
     * @param keyword tìm theo mã đơn / email / tên khách / tên khách sạn (null = bỏ qua)
     */
    List<HotelBookingEntity> findForAdmin(String status, String keyword, int page, int limit);

    long countForAdmin(String status, String keyword);
}
