package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AuditLogDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AuditLogService {
    /** Ghi 1 dòng log trong transaction riêng; lỗi ghi log không được làm hỏng request. */
    void record(AuditEntry entry);

    /** action: khớp chính xác hoặc tiền tố (VD "hotels" khớp "hotels.update"); from/to tính cả 2 đầu. */
    Page<AuditLogDTO> search(UUID userId, String action, LocalDate from, LocalDate to, int page, int limit);

    List<String> actions();

    record AuditEntry(UUID userId, String userEmail, String action, String httpMethod, String path, String entityType,
                      String entityId, Integer statusCode, String ipAddress, String userAgent, Map<String, Object> details) {
    }
}
