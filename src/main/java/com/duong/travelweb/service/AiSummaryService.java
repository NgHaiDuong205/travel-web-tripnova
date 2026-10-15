package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AiSummarizeResultDTO;
import com.duong.travelweb.model.dto.AiSummaryDTO;
import org.springframework.data.domain.Page;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

/**
 * Tóm tắt review bằng AI (ai_summaries, summary_type = reviews): sinh nền qua hàng đợi ai_jobs, một lượt gọi AI
 * ra cả bản vi + en. Bản tóm tắt "cũ" khi số review đã duyệt khác số review lúc sinh.
 */
public interface AiSummaryService {
    String SUMMARY_TYPE_REVIEWS = "reviews";
    String JOB_SUMMARIZE_REVIEWS = "summarize_reviews";

    /** Bản tóm tắt hiện có (null nếu chưa có hoặc số review đã duyệt dưới ngưỡng). language: vi | en. */
    AiSummaryDTO getReviewSummary(String entityType, UUID entityId, String language);

    /**
     * Khách yêu cầu tóm tắt: status fresh (bản hiện tại còn đúng) | not_enough_reviews | queued (đã đưa vào hàng đợi,
     * kèm jobId và bản cũ nếu có).
     */
    AiSummarizeResultDTO requestReviewSummary(String entityType, UUID entityId, String language);

    /** Gọi trong transaction thay đổi review đã duyệt (duyệt, gỡ duyệt, xoá, admin sửa) → xếp job sinh lại. */
    void reviewsChanged(String entityType, UUID entityId);

    /** Admin: sinh lại ngay cả khi bản hiện tại còn đúng. Trả id job. */
    UUID regenerate(String entityType, UUID entityId);

    /** Xếp job cho đối tượng thiếu / cũ / không còn đủ review (bỏ qua đối tượng vừa lỗi trong 24h). Trả số job mới xếp. */
    int enqueueStale(int limit);

    Page<AiSummaryDTO> search(String entityType, String language, int page, int limit);

    // ----- dùng bởi job

    /**
     * Dữ liệu gửi dịch vụ AI; null nếu không còn đủ review hoặc đối tượng không còn — khi đó xoá bản tóm tắt cũ.
     */
    ReviewRequest buildReviewRequest(String entityType, UUID entityId);

    void saveReviewSummaries(String entityType, UUID entityId, int basedOnCount, JsonNode aiResult);

    /** body: theo summaries.ReviewSummaryRequest bên Python; reviewCount: tổng review đã duyệt lúc lấy. */
    record ReviewRequest(Map<String, Object> body, int reviewCount) {
    }
}
