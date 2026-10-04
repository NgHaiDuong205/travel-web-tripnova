package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.ChatFeedbackRequestDTO;
import com.duong.travelweb.model.dto.ChatMessageDTO;
import com.duong.travelweb.model.dto.ChatReplyDTO;
import com.duong.travelweb.model.dto.ChatSendRequestDTO;
import com.duong.travelweb.model.dto.ChatSessionCreateRequestDTO;
import com.duong.travelweb.model.dto.ChatSessionDTO;
import com.duong.travelweb.service.AiChatService;
import com.duong.travelweb.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Trợ lý AI (Text-to-SQL + RAG) cho khách đã đăng nhập. Mọi phiên chỉ chủ phiên xem được. */
@RestController
public class AiChatAPI {
    private static final int MAX_LIMIT = 50;

    private final AiChatService aiChatService;

    public AiChatAPI(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /** Body tuỳ chọn {contextType: hotel|tour|car|flight|destination|landmark, contextId} khi mở từ trang chi tiết. */
    @PostMapping("/api/ai/chat/sessions/")
    public ResponseEntity<ChatSessionDTO> createSession(@RequestBody(required = false) ChatSessionCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(aiChatService.createSession(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/api/ai/chat/sessions/")
    public ResponseEntity<List<ChatSessionDTO>> getSessions(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<ChatSessionDTO> result = aiChatService.findSessions(SecurityUtil.getCurrentUserId(), page,
                Math.min(Math.max(limit, 1), MAX_LIMIT));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @DeleteMapping("/api/ai/chat/sessions/{sessionId}/")
    public ResponseEntity<Void> closeSession(@PathVariable("sessionId") UUID sessionId) {
        aiChatService.closeSession(SecurityUtil.getCurrentUserId(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/ai/chat/sessions/{sessionId}/messages/")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable("sessionId") UUID sessionId) {
        return ResponseEntity.ok(aiChatService.getMessages(SecurityUtil.getCurrentUserId(), sessionId));
    }

    /** {content, locale: vi|en} → câu hỏi + câu trả lời (lỗi AI = assistantMessage.isError). 429 khi hỏi quá nhiều. */
    @PostMapping("/api/ai/chat/sessions/{sessionId}/messages/")
    public ResponseEntity<ChatReplyDTO> sendMessage(@PathVariable("sessionId") UUID sessionId,
                                                    @RequestBody ChatSendRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(aiChatService.sendMessage(SecurityUtil.getCurrentUserId(), sessionId, request));
    }

    /** {rating?: 1-5, isHelpful?, comment?}; gửi lại thì ghi đè. */
    @PostMapping("/api/ai/chat/messages/{messageId}/feedback/")
    public ResponseEntity<ChatMessageDTO> sendFeedback(@PathVariable("messageId") UUID messageId,
                                                       @RequestBody ChatFeedbackRequestDTO request) {
        return ResponseEntity.ok(aiChatService.sendFeedback(SecurityUtil.getCurrentUserId(), messageId, request));
    }
}
