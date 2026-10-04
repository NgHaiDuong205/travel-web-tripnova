package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.PlannerRequestDTO;
import com.duong.travelweb.model.dto.PlannerResultDTO;

import java.util.UUID;

/** AI Planner: dựng bản nháp lịch trình từ dữ liệu thật (toạ độ) + AI viết/chọn trong ứng viên. Không lưu DB. */
public interface ItineraryPlannerService {
    PlannerResultDTO plan(UUID userId, PlannerRequestDTO request);
}
