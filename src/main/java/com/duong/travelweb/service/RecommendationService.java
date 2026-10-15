package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.RecommendationDTO;

import java.util.List;
import java.util.UUID;

/**
 * Gợi ý cá nhân hoá (bảng recommendations, algorithm = hybrid) và khách sạn tương tự (cache similar_items).
 * Thuật toán chấm điểm trên dữ liệu thật — không dùng LLM, nên không tốn hạn mức AI và không bịa địa điểm.
 */
public interface RecommendationService {
    List<String> TYPES = List.of("hotel", "tour", "destination", "landmark", "car");

    /**
     * Gợi ý cho user (type: all | hotel | tour | destination | landmark | car). Hết hạn (1 giờ) hoặc tín hiệu thay đổi
     * → tính lại. type = all trộn xen kẽ các loại.
     */
    List<RecommendationDTO> recommend(UUID userId, String type, int limit);

    /** Đánh dấu gợi ý của user là cũ (vừa thêm/bỏ yêu thích, đặt chỗ...) — lần đọc sau tính lại. */
    void invalidate(UUID userId);

    /** Ghi tương tác (view | click | share). Cùng user + đối tượng + hành động trong 30 phút chỉ ghi 1 lần. */
    void recordInteraction(UUID userId, String entityType, UUID entityId, String action);

    /** Khách sạn tương tự (cùng điểm đến: tiện nghi, giá, hạng sao, khoảng cách), cache 7 ngày. */
    List<RecommendationDTO> similarHotels(UUID hotelId, int limit);
}
