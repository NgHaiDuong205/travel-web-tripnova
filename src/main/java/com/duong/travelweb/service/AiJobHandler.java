package com.duong.travelweb.service;

import tools.jackson.databind.JsonNode;

/**
 * Xử lý một loại job trong hàng đợi ai_jobs. Chạy ngoài transaction (thường gọi dịch vụ AI vài giây) — tự mở
 * transaction ngắn khi đọc/ghi DB. Ném AiServiceClient.AiException 503 (hết hạn mức, AI không kết nối được)
 * thì job được trả về hàng đợi không tính lượt; ApiException 4xx = lỗi vĩnh viễn (không thử lại); lỗi khác thử lại.
 */
public interface AiJobHandler {
    String jobType();

    void handle(JsonNode payload);
}
