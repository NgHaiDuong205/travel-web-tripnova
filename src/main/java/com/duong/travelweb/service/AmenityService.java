package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AdminAmenityDTO;
import com.duong.travelweb.model.dto.AmenityDTO;
import com.duong.travelweb.model.dto.AmenityRequestDTO;

import java.util.List;
import java.util.UUID;

public interface AmenityService {
    List<AmenityDTO> findAmenities(String category);

    List<AdminAmenityDTO> findForAdmin(String category);
    AdminAmenityDTO create(AmenityRequestDTO request);
    AdminAmenityDTO update(UUID amenityId, AmenityRequestDTO request);
    /** force = false: tiện ích đang được dùng → 409. force = true: xoá kèm liên kết (DB ON DELETE CASCADE). */
    void delete(UUID amenityId, boolean force);
}
