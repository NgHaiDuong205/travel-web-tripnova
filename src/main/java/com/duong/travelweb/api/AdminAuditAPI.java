package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AuditLogDTO;
import com.duong.travelweb.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Nhật ký thao tác (ROLE_ADMIN). */
@RestController
public class AdminAuditAPI {
    private static final int MAX_LIMIT = 100;

    private final AuditLogService auditLogService;

    public AdminAuditAPI(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /** ?userId&action (chính xác hoặc tiền tố, VD "hotels")&from&to (yyyy-MM-dd, tính cả 2 đầu); mới nhất trước. */
    @GetMapping("/api/admin/audit-logs/")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogs(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<AuditLogDTO> result = auditLogService.search(userId, action, from, to, page, Math.min(Math.max(limit, 1), MAX_LIMIT));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @GetMapping("/api/admin/audit-logs/actions/")
    public ResponseEntity<List<String>> getActions() {
        return ResponseEntity.ok(auditLogService.actions());
    }
}
