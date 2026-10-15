package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.InteractionRequestDTO;
import com.duong.travelweb.model.dto.RecommendationDTO;
import com.duong.travelweb.service.RecommendationService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Gợi ý cá nhân hoá, ghi tương tác làm tín hiệu, khách sạn tương tự. */
@RestController
public class RecommendationAPI {
    private static final int MAX_LIMIT = 30;

    private final RecommendationService recommendationService;

    public RecommendationAPI(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /** [AUTH] ?type=all|hotel|tour|destination|landmark|car&limit (mặc định 12, tối đa 30). */
    @GetMapping("/api/ai/recommendations/")
    public ResponseEntity<List<RecommendationDTO>> recommendations(
            @RequestParam(value = "type", defaultValue = "all") String type,
            @RequestParam(value = "limit", defaultValue = "12") int limit) {
        return ResponseEntity.ok(recommendationService.recommend(SecurityUtil.getCurrentUserId(), type,
                Math.min(Math.max(limit, 1), MAX_LIMIT)));
    }

    /** [AUTH] {entityType, entityId, action: view|click|share} → 204; cùng mục trong 30 phút chỉ ghi 1 lần. */
    @PostMapping("/api/interactions/")
    public ResponseEntity<Void> recordInteraction(@Valid @RequestBody InteractionRequestDTO request) {
        recommendationService.recordInteraction(SecurityUtil.getCurrentUserId(), request.getEntityType(),
                request.getEntityId(), request.getAction());
        return ResponseEntity.noContent().build();
    }

    /** Public. Khách sạn tương tự (cùng điểm đến), kèm distanceKm khi có toạ độ. */
    @GetMapping("/api/hotels/{hotelId}/similar/")
    public ResponseEntity<List<RecommendationDTO>> similarHotels(@PathVariable("hotelId") UUID hotelId,
                                                                 @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(recommendationService.similarHotels(hotelId, Math.min(Math.max(limit, 1), 12)));
    }
}
