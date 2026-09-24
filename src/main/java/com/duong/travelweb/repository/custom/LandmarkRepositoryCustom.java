package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.LandmarkEntity;

import java.util.List;
import java.util.UUID;

public interface LandmarkRepositoryCustom {
    /** Danh sách cho admin (kể cả đã ẩn). Tham số null = bỏ qua điều kiện. */
    List<LandmarkEntity> findForAdmin(String keyword, UUID destinationId, String category, Boolean active, int page, int limit);
    long countForAdmin(String keyword, UUID destinationId, String category, Boolean active);
}
