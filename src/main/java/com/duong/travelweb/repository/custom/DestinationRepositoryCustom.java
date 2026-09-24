package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.DestinationEntity;

import java.util.List;
import java.util.UUID;

public interface DestinationRepositoryCustom {
    /** Danh sách cho admin (kể cả đã ẩn). Tham số null = bỏ qua điều kiện. */
    List<DestinationEntity> findForAdmin(String keyword, UUID countryId, Boolean active, int page, int limit);
    long countForAdmin(String keyword, UUID countryId, Boolean active);
}
