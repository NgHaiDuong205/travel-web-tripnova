package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.entity.AiJobEntity;
import com.duong.travelweb.service.AiJobHandler;
import com.duong.travelweb.service.AiJobService;
import com.duong.travelweb.service.AiSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chạy job trong ai_jobs, mỗi nhịp 1 job (gói AI miễn phí giới hạn số request/phút).
 * - Hết hạn mức / dịch vụ AI không kết nối được / chưa cấu hình (503): trả job về hàng đợi KHÔNG tính lượt và tạm
 *   dừng worker (hết hạn mức 10', còn lại 2') — job không bị đánh lỗi chỉ vì AI tạm vắng.
 * - Lỗi 4xx (dữ liệu không hợp lệ): failed luôn. Lỗi khác: thử lại sau attempts × backoff phút, hết lượt → failed.
 * - Job running quá lâu (BE tắt giữa chừng) được đưa về pending.
 */
@Component
public class AiJobWorker {
    private static final Logger log = LoggerFactory.getLogger(AiJobWorker.class);

    private final AiJobService aiJobService;
    private final AiSummaryService aiSummaryService;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final Map<String, AiJobHandler> handlers = new LinkedHashMap<>();
    private final boolean enabled;
    private final int maxAttempts;
    private final int retryBackoffMinutes;
    private final int quotaPauseMinutes;
    private final int stuckMinutes;
    private volatile Instant pausedUntil;
    private volatile String pauseReason;
    private volatile Instant lastRecovery = Instant.EPOCH;

    public AiJobWorker(AiJobService aiJobService, AiSummaryService aiSummaryService, AiServiceClient aiServiceClient,
                       ObjectMapper objectMapper, List<AiJobHandler> handlerList,
                       @Value("${app.ai.jobs.enabled:true}") boolean enabled,
                       @Value("${app.ai.jobs.max-attempts:3}") int maxAttempts,
                       @Value("${app.ai.jobs.retry-backoff-minutes:5}") int retryBackoffMinutes,
                       @Value("${app.ai.jobs.quota-pause-minutes:10}") int quotaPauseMinutes,
                       @Value("${app.ai.jobs.stuck-minutes:15}") int stuckMinutes) {
        this.aiJobService = aiJobService;
        this.aiSummaryService = aiSummaryService;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
        handlerList.forEach(h -> handlers.put(h.jobType(), h));
        this.enabled = enabled;
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.retryBackoffMinutes = Math.max(retryBackoffMinutes, 0);
        this.quotaPauseMinutes = Math.max(quotaPauseMinutes, 1);
        this.stuckMinutes = Math.max(stuckMinutes, 2);
    }

    @Scheduled(fixedDelayString = "${app.ai.jobs.poll-ms:5000}", initialDelayString = "${app.ai.jobs.initial-delay-ms:20000}")
    public void tick() {
        if (!enabled || !aiServiceClient.isConfigured() || isPaused()) {
            return;
        }
        if (lastRecovery.isBefore(Instant.now().minus(1, ChronoUnit.MINUTES))) {
            lastRecovery = Instant.now();
            int recovered = aiJobService.recoverStuck(stuckMinutes, maxAttempts);
            if (recovered > 0) {
                log.warn("AI jobs: recovered {} stuck running job(s)", recovered);
            }
        }
        AiJobEntity job = aiJobService.claimNext(handlers.keySet(), retryBackoffMinutes);
        if (job != null) {
            run(job);
        }
    }

    /** Mỗi 30 phút (và 2 phút sau khi khởi động) xếp job cho bản tóm tắt thiếu / cũ — bù cho thay đổi không qua hook. */
    @Scheduled(fixedDelayString = "${app.ai.summaries.sweep-ms:1800000}", initialDelayString = "${app.ai.summaries.sweep-initial-delay-ms:120000}")
    public void sweepSummaries() {
        if (!enabled) {
            return;
        }
        int queued = aiSummaryService.enqueueStale(200);
        if (queued > 0) {
            log.info("AI jobs: queued {} review summary job(s)", queued);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getPausedUntil() {
        return isPaused() ? pausedUntil : null;
    }

    public String getPauseReason() {
        return isPaused() ? pauseReason : null;
    }

    private boolean isPaused() {
        Instant until = pausedUntil;
        return until != null && Instant.now().isBefore(until);
    }

    private void run(AiJobEntity job) {
        AiJobHandler handler = handlers.get(job.getJobType());
        int attempts = job.getAttempts() == null ? 1 : job.getAttempts();
        try {
            JsonNode payload = job.getPayload() == null ? null : objectMapper.readTree(job.getPayload());
            handler.handle(payload);
            aiJobService.complete(job.getId());
        } catch (AiServiceClient.AiException ex) {
            if (ex.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
                boolean quota = AiServiceClient.ERR_QUOTA.equals(ex.getCode());
                pause(quota ? quotaPauseMinutes : 2, ex.getCode());
                aiJobService.release(job.getId(), ex.getCode() + ": " + ex.getMessage());
                log.warn("AI jobs: paused ({}), job {} returned to queue", ex.getCode(), job.getId());
                return;
            }
            fail(job, attempts, ex.getCode() + ": " + ex.getMessage(), ex.getStatus().is4xxClientError());
        } catch (ApiException ex) {
            fail(job, attempts, ex.getMessage(), ex.getStatus().is4xxClientError());
        } catch (IllegalArgumentException ex) {
            fail(job, attempts, ex.getMessage(), true);
        } catch (RuntimeException ex) {
            log.warn("AI job {} ({}) failed: {}", job.getId(), job.getJobType(), ex.toString());
            fail(job, attempts, ex.getClass().getSimpleName() + ": " + ex.getMessage(), false);
        }
    }

    private void fail(AiJobEntity job, int attempts, String error, boolean permanent) {
        aiJobService.fail(job.getId(), error, permanent || attempts >= maxAttempts);
    }

    private void pause(int minutes, String reason) {
        pausedUntil = Instant.now().plus(minutes, ChronoUnit.MINUTES);
        pauseReason = reason;
    }
}
