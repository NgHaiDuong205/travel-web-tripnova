package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.CarSearchBuilder;
import com.duong.travelweb.model.entity.CarEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface CarRepositoryCustom {
    /**
     * Danh sách xe theo bộ lọc, kèm sẵn destination + country.
     * @param holdCutoff booking pending tạo sau mốc này vẫn đang giữ xe (dùng khi lọc theo khoảng ngày trống)
     */
    List<CarEntity> findCars(CarSearchBuilder criteria, LocalDateTime holdCutoff, int page, int limit);

    long countCars(CarSearchBuilder criteria, LocalDateTime holdCutoff);
}
