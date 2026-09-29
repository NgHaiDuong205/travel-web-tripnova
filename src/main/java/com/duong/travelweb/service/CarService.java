package com.duong.travelweb.service;

import com.duong.travelweb.builder.CarSearchBuilder;
import com.duong.travelweb.model.dto.CarDTO;
import com.duong.travelweb.model.dto.CarFilterOptionsDTO;
import com.duong.travelweb.model.dto.CarRequestDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/** Danh mục xe cho thuê (public) và quản trị xe. */
public interface CarService {
    List<String> CAR_TYPES = List.of("sedan", "suv", "minivan", "luxury", "motorbike", "coach");

    Page<CarDTO> search(CarSearchBuilder criteria, int page, int limit);

    /** Xe đang cho thuê, ngược lại 404. */
    CarDTO getPublic(UUID carId);

    CarFilterOptionsDTO filterOptions();

    // ---- Admin ----
    CarDTO get(UUID carId);

    CarDTO create(CarRequestDTO request);

    CarDTO update(UUID carId, CarRequestDTO request);

    /** Xoá mềm (is_active = false): booking cũ giữ nguyên, không nhận đặt mới. */
    void deactivate(UUID carId);
}
