package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AdminChatMessageDTO;
import com.duong.travelweb.model.dto.AdminChatSessionDTO;
import com.duong.travelweb.model.dto.KnowledgeDocumentDTO;
import com.duong.travelweb.service.AdminAiService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Quản trị trợ lý AI (ROLE_ADMIN): kho tài liệu RAG, lập chỉ mục lại, xem phiên chat của khách. */
@RestController
public class AdminAiAPI {
    private static final int MAX_LIMIT = 100;

    private final AdminAiService adminAiService;

    public AdminAiAPI(AdminAiService adminAiService) {
        this.adminAiService = adminAiService;
    }

    /** ?q (tiêu đề)&sourceType&language=vi|en&active&page&limit — mới cập nhật trước. */
    @GetMapping("/api/admin/ai/knowledge-documents/")
    public ResponseEntity<List<KnowledgeDocumentDTO>> getDocuments(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<KnowledgeDocumentDTO> result = adminAiService.searchDocuments(q, sourceType, language, active, page,
                Math.min(Math.max(limit, 1), MAX_LIMIT));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @GetMapping("/api/admin/ai/knowledge-documents/{documentId}/")
    public ResponseEntity<KnowledgeDocumentDTO> getDocument(@PathVariable("documentId") UUID documentId) {
        return ResponseEntity.ok(adminAiService.getDocument(documentId));
    }

    /**
     * multipart: file (.txt/.md/.pdf ≤ 5 MB), title?, sourceType (policy|faq|guide|hotel|tour|car|flight|destination|other),
     * sourceId? (id đối tượng áp dụng), language (vi|en), url?. Cùng title + nguồn → phiên bản mới, bản cũ tắt.
     */
    @PostMapping(value = "/api/admin/ai/knowledge-documents/", consumes = "multipart/form-data")
    public ResponseEntity<KnowledgeDocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "sourceId", required = false) UUID sourceId,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "url", required = false) String url) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminAiService.uploadDocument(file, title, sourceType, sourceId, language, url));
    }

    /** {isActive} */
    @PatchMapping("/api/admin/ai/knowledge-documents/{documentId}/")
    public ResponseEntity<KnowledgeDocumentDTO> setActive(@PathVariable("documentId") UUID documentId,
                                                          @RequestBody Map<String, Boolean> body) {
        Boolean active = body == null ? null : body.get("isActive");
        if (active == null) {
            throw com.duong.travelweb.exception.ApiException.badRequest("Thiếu isActive");
        }
        return ResponseEntity.ok(adminAiService.setDocumentActive(documentId, active));
    }

    @DeleteMapping("/api/admin/ai/knowledge-documents/{documentId}/")
    public ResponseEntity<Void> deleteDocument(@PathVariable("documentId") UUID documentId) {
        adminAiService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    /** ?full=false: chỉ tài liệu lập chỉ mục bằng model embedding cũ; full=true: tất cả tài liệu đang bật. */
    @PostMapping("/api/admin/ai/jobs/reindex/")
    public ResponseEntity<Map<String, Object>> reindex(@RequestParam(value = "full", defaultValue = "false") boolean full) {
        return ResponseEntity.ok(adminAiService.reindex(full));
    }

    /** ?userId&q (tiêu đề / email / tên)&from&to (yyyy-MM-dd theo ngày cập nhật, tính cả 2 đầu)&page&limit. */
    @GetMapping("/api/admin/ai/chat-sessions/")
    public ResponseEntity<List<AdminChatSessionDTO>> getSessions(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<AdminChatSessionDTO> result = adminAiService.searchSessions(userId, q, from, to, page,
                Math.min(Math.max(limit, 1), MAX_LIMIT));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    /** Tin nhắn của phiên kèm SQL đã sinh, số dòng, nguồn tài liệu, model, token, thời gian (để kiểm tra chất lượng). */
    @GetMapping("/api/admin/ai/chat-sessions/{sessionId}/messages/")
    public ResponseEntity<List<AdminChatMessageDTO>> getSessionMessages(@PathVariable("sessionId") UUID sessionId) {
        return ResponseEntity.ok(adminAiService.getSessionMessages(sessionId));
    }
}
