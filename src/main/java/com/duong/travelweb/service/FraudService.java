package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.FraudScoreDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chấm điểm rủi ro gian lận cho giao dịch (fraud_scores, entity_type = payment) bằng các quy tắc giải thích được.
 * Chỉ gắn cờ để admin xem xét — không tự chặn khách.
 */
public interface FraudService {
    List<String> LEVELS = List.of("low", "medium", "high");
    List<String> ACTIONS = List.of("allow", "review", "cleared", "confirmed_fraud");

    /**
     * Chấm điểm 1 giao dịch (đã có điểm → trả bản cũ). Mốc thời gian = lúc tạo giao dịch nên chấm lại đơn cũ vẫn đúng.
     * clientIp: IP của request đặt chỗ (null nếu không có, VD chấm bù) — chỉ dùng để đếm, không lưu.
     */
    FraudScoreDTO scorePayment(UUID paymentId, String clientIp);

    Page<FraudScoreDTO> search(String level, String action, String q, int page, int limit);

    FraudScoreDTO get(UUID id);

    /** Admin đánh dấu: review | cleared (hợp lệ) | confirmed_fraud. */
    FraudScoreDTO updateAction(UUID id, String action);

    /** Số giao dịch theo mức rủi ro + số đang chờ xem xét. */
    Map<String, Long> stats();

    /** Chấm bù các giao dịch chưa có điểm (mới nhất trước, tối đa limit). Trả số đã chấm. */
    int backfill(int limit);
}
