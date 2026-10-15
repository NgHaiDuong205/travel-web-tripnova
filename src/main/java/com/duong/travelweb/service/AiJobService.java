package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AiJobDTO;
import com.duong.travelweb.model.entity.AiJobEntity;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hàng đợi job AI chạy nền (bảng ai_jobs): pending → running → done | failed; admin huỷ (cancelled) / chạy lại.
 * Nhận job bằng FOR UPDATE SKIP LOCKED nên chạy nhiều instance BE cũng không trùng.
 */
public interface AiJobService {
    List<String> STATUSES = List.of("pending", "running", "done", "failed", "cancelled");

    /**
     * Thêm job trong transaction hiện tại. Đã có job pending cùng loại + payload → không thêm, trả id job đó
     * (gom các thay đổi liên tiếp của cùng một đối tượng thành 1 lượt gọi AI).
     */
    UUID enqueue(String jobType, Map<String, Object> payload);

    Page<AiJobDTO> search(String status, String jobType, UUID entityId, int page, int limit);

    AiJobDTO get(UUID id);

    /** failed | cancelled → pending (đếm lượt lại từ 0). */
    AiJobDTO retry(UUID id);

    /** pending → cancelled. */
    AiJobDTO cancel(UUID id);

    Map<String, Long> countByStatus();

    // ----- dùng bởi worker

    /** Nhận 1 job pending (đã qua thời gian chờ thử lại) thuộc các loại cho trước: running, attempts + 1. */
    AiJobEntity claimNext(Collection<String> jobTypes, int retryBackoffMinutes);

    void complete(UUID id);

    /** finalFailure: failed; ngược lại về pending, chờ thử lại theo backoff. */
    void fail(UUID id, String error, boolean finalFailure);

    /** Trả job về pending và hoàn lại lượt vừa tính (lỗi tạm thời không do job: hết hạn mức, AI tắt). */
    void release(UUID id, String note);

    /** Job running quá lâu (BE tắt giữa chừng) → pending, hoặc failed nếu đã hết lượt. Trả số job xử lý. */
    int recoverStuck(int staleMinutes, int maxAttempts);
}
