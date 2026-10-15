package com.duong.travelweb.service.impl;

import com.duong.travelweb.service.AiJobHandler;
import com.duong.travelweb.service.AiSummaryService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Job summarize_reviews {entityType, entityId}: đọc review đã duyệt (transaction ngắn) → gọi dịch vụ AI (ngoài
 * transaction) → ghi bản vi + en. Luôn sinh theo dữ liệu tại lúc chạy, nên nhiều thay đổi dồn lại chỉ cần 1 lượt.
 */
@Component
public class ReviewSummaryJobHandler implements AiJobHandler {
    private final AiSummaryService aiSummaryService;
    private final AiServiceClient aiServiceClient;

    public ReviewSummaryJobHandler(AiSummaryService aiSummaryService, AiServiceClient aiServiceClient) {
        this.aiSummaryService = aiSummaryService;
        this.aiServiceClient = aiServiceClient;
    }

    @Override
    public String jobType() {
        return AiSummaryService.JOB_SUMMARIZE_REVIEWS;
    }

    @Override
    public void handle(JsonNode payload) {
        if (payload == null || !payload.hasNonNull("entityType") || !payload.hasNonNull("entityId")) {
            throw new IllegalArgumentException("payload thiếu entityType / entityId");
        }
        String entityType = payload.get("entityType").asString();
        UUID entityId = UUID.fromString(payload.get("entityId").asString());
        AiSummaryService.ReviewRequest request = aiSummaryService.buildReviewRequest(entityType, entityId);
        if (request == null) {
            return; // không còn đủ review: bản cũ đã bị xoá
        }
        JsonNode result = aiServiceClient.summarizeReviews(request.body());
        aiSummaryService.saveReviewSummaries(entityType, entityId, request.reviewCount(), result);
    }
}
