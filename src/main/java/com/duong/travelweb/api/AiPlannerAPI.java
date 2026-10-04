package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.ItineraryDTO;
import com.duong.travelweb.model.dto.PlannerRequestDTO;
import com.duong.travelweb.model.dto.PlannerResultDTO;
import com.duong.travelweb.model.dto.PlannerSaveRequestDTO;
import com.duong.travelweb.service.ItineraryPlannerService;
import com.duong.travelweb.service.ItineraryService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** AI Planner (đăng nhập): tạo bản nháp lịch trình từ điểm đến, rồi lưu vào /api/me/itineraries. */
@RestController
public class AiPlannerAPI {
    private final ItineraryPlannerService plannerService;
    private final ItineraryService itineraryService;

    public AiPlannerAPI(ItineraryPlannerService plannerService, ItineraryService itineraryService) {
        this.plannerService = plannerService;
        this.itineraryService = itineraryService;
    }

    /** Bản nháp (không lưu). AI lỗi vẫn trả 200 với warnings ai_unavailable|ai_quota; 429 khi tạo quá nhiều. */
    @PostMapping("/api/ai/itineraries/plan/")
    public ResponseEntity<PlannerResultDTO> plan(@Valid @RequestBody PlannerRequestDTO request) {
        return ResponseEntity.ok(plannerService.plan(SecurityUtil.getCurrentUserId(), request));
    }

    /** Lưu bản nháp (có thể đã chỉnh) thành lịch trình generated_by = ai. */
    @PostMapping("/api/ai/itineraries/")
    public ResponseEntity<ItineraryDTO> save(@Valid @RequestBody PlannerSaveRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.createFromPlanner(SecurityUtil.getCurrentUserId(), request));
    }
}
