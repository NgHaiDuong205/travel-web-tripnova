package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AiJobDTO;
import com.duong.travelweb.model.dto.AiSummarizeRequestDTO;
import com.duong.travelweb.model.dto.AiSummaryDTO;
import com.duong.travelweb.service.AiJobService;
import com.duong.travelweb.service.AiSummaryService;
import com.duong.travelweb.service.impl.AiJobWorker;
import com.duong.travelweb.service.impl.AiServiceClient;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Quản trị hàng đợi job AI (ai_jobs) và bản tóm tắt review (ai_summaries) — ROLE_ADMIN. */
@RestController
public class AdminAiJobAPI {
    private static final int MAX_LIMIT = 100;

    private final AiJobService aiJobService;
    private final AiSummaryService aiSummaryService;
    private final AiJobWorker aiJobWorker;
    private final AiServiceClient aiServiceClient;

    public AdminAiJobAPI(AiJobService aiJobService, AiSummaryService aiSummaryService, AiJobWorker aiJobWorker,
                         AiServiceClient aiServiceClient) {
        this.aiJobService = aiJobService;
        this.aiSummaryService = aiSummaryService;
        this.aiJobWorker = aiJobWorker;
        this.aiServiceClient = aiServiceClient;
    }

    /** ?status=pending|running|done|failed|cancelled&type&entityId&page&limit — mới tạo trước. */
    @GetMapping("/api/admin/ai/jobs/")
    public ResponseEntity<List<AiJobDTO>> getJobs(@RequestParam(value = "status", required = false) String status,
                                                  @RequestParam(value = "type", required = false) String type,
                                                  @RequestParam(value = "entityId", required = false) UUID entityId,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<AiJobDTO> result = aiJobService.search(status, type, entityId, page, Math.min(Math.max(limit, 1), MAX_LIMIT));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    /** Số job theo trạng thái + tình trạng worker (tắt, AI chưa cấu hình, đang tạm dừng vì hết hạn mức...). */
    @GetMapping("/api/admin/ai/jobs/stats/")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("counts", aiJobService.countByStatus());
        body.put("workerEnabled", aiJobWorker.isEnabled());
        body.put("aiConfigured", aiServiceClient.isConfigured());
        body.put("pausedUntil", aiJobWorker.getPausedUntil());
        body.put("pauseReason", aiJobWorker.getPauseReason());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/admin/ai/jobs/{jobId}/")
    public ResponseEntity<AiJobDTO> getJob(@PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(aiJobService.get(jobId));
    }

    @PostMapping("/api/admin/ai/jobs/{jobId}/retry/")
    public ResponseEntity<AiJobDTO> retryJob(@PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(aiJobService.retry(jobId));
    }

    @PostMapping("/api/admin/ai/jobs/{jobId}/cancel/")
    public ResponseEntity<AiJobDTO> cancelJob(@PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(aiJobService.cancel(jobId));
    }

    /** ?entityType&lang=vi|en&page&limit — mới sinh trước, kèm tên đối tượng + số review hiện tại (stale). */
    @GetMapping("/api/admin/ai/summaries/")
    public ResponseEntity<List<AiSummaryDTO>> getSummaries(
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "lang", required = false) String lang,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<AiSummaryDTO> result = aiSummaryService.search(entityType, lang, page, Math.min(Math.max(limit, 1), MAX_LIMIT));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    /** {entityType, entityId} → 202 {jobId}: sinh lại kể cả khi bản hiện tại còn đúng. */
    @PostMapping("/api/admin/ai/summaries/regenerate/")
    public ResponseEntity<Map<String, Object>> regenerate(@Valid @RequestBody AiSummarizeRequestDTO request) {
        UUID jobId = aiSummaryService.regenerate(request.getEntityType(), request.getEntityId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));
    }

    /** Xếp job cho mọi đối tượng thiếu / cũ (tối đa 500 mỗi lần). */
    @PostMapping("/api/admin/ai/summaries/backfill/")
    public ResponseEntity<Map<String, Object>> backfill() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("queued", aiSummaryService.enqueueStale(500)));
    }
}
