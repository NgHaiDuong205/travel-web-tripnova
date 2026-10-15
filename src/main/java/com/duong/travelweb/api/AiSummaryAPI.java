package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AiSummarizeRequestDTO;
import com.duong.travelweb.model.dto.AiSummarizeResultDTO;
import com.duong.travelweb.model.dto.AiSummaryDTO;
import com.duong.travelweb.service.AiSummaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Tóm tắt review bằng AI cho trang chi tiết (khách sạn, tour, xe, chuyến bay, điểm đến, địa danh). */
@RestController
public class AiSummaryAPI {
    private final AiSummaryService aiSummaryService;

    public AiSummaryAPI(AiSummaryService aiSummaryService) {
        this.aiSummaryService = aiSummaryService;
    }

    /** Public. ?entityType&entityId&lang=vi|en → 200 bản tóm tắt (kèm stale nếu đã có review mới), 204 nếu chưa có. */
    @GetMapping("/api/ai/summaries/")
    public ResponseEntity<AiSummaryDTO> getSummary(@RequestParam("entityType") String entityType,
                                                   @RequestParam("entityId") UUID entityId,
                                                   @RequestParam(value = "lang", required = false) String lang) {
        AiSummaryDTO summary = aiSummaryService.getReviewSummary(entityType, entityId, lang);
        return summary == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(summary);
    }

    /**
     * [AUTH] {entityType, entityId} ?lang → 200 {status: fresh | not_enough_reviews, summary?}
     * hoặc 202 {status: queued, jobId, summary? (bản cũ)} — sinh nền, đọc lại bằng GET sau ít giây.
     */
    @PostMapping("/api/ai/summarize/")
    public ResponseEntity<AiSummarizeResultDTO> summarize(@Valid @RequestBody AiSummarizeRequestDTO request,
                                                          @RequestParam(value = "lang", required = false) String lang) {
        AiSummarizeResultDTO result = aiSummaryService.requestReviewSummary(request.getEntityType(),
                request.getEntityId(), lang);
        return ResponseEntity.status("queued".equals(result.getStatus()) ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(result);
    }
}
