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
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Trợ lý AI (Text-to-SQL + RAG) cho khách đã đăng nhập. Mọi phiên chỉ chủ phiên xem được. */
@RestController
public class AiChatAPI {
    private static final Logger log = LoggerFactory.getLogger(AiChatAPI.class);
    private static final int MAX_LIMIT = 50;
    private static final long STREAM_TIMEOUT_MS = 180_000;

    private final AiChatService aiChatService;
    /** Mỗi câu trả lời stream chờ AI vài giây → virtual thread, không chiếm thread của Tomcat. */
    private final ExecutorService streams = Executors.newVirtualThreadPerTaskExecutor();

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

    /**
     * Như sendMessage nhưng trả lời dạng Server-Sent Events: "delta" {text}… rồi "done" (ChatReplyDTO, như sendMessage;
     * AI lỗi → assistantMessage.isError). Lỗi kiểm tra (rỗng, 429, không có phiên) trả JSON thường trước khi stream.
     * Khách ngắt giữa chừng thì câu trả lời vẫn được lưu đủ.
     */
    @PostMapping("/api/ai/chat/sessions/{sessionId}/messages/stream/")
    public SseEmitter sendMessageStream(@PathVariable("sessionId") UUID sessionId,
                                        @RequestBody ChatSendRequestDTO request) {
        AiChatService.PendingQuestion pending = aiChatService.submitQuestion(SecurityUtil.getCurrentUserId(), sessionId, request);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        AtomicBoolean open = new AtomicBoolean(true);
        emitter.onTimeout(() -> open.set(false));
        emitter.onError(ex -> open.set(false));
        streams.execute(() -> {
            try {
                ChatReplyDTO reply = aiChatService.answerStreaming(pending, text -> send(emitter, open, "delta", Map.of("text", text)));
                send(emitter, open, "done", reply);
            } catch (RuntimeException ex) {
                // VD phiên bị đóng ở tab khác giữa chừng: báo lỗi trong stream, FE tải lại cuộc trò chuyện.
                log.warn("Chat stream for session {} failed: {}", sessionId, ex.getMessage());
                send(emitter, open, "error", Map.of("message", ex.getMessage() == null ? "error" : ex.getMessage()));
            } finally {
                if (open.get()) {
                    emitter.complete();
                }
            }
        });
        return emitter;
    }

    private static void send(SseEmitter emitter, AtomicBoolean open, String name, Object data) {
        if (!open.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            open.set(false); // khách đã đóng kết nối
        }
    }

    @PreDestroy
    void shutdown() {
        streams.shutdown();
    }

    /** {rating?: 1-5, isHelpful?, comment?}; gửi lại thì ghi đè. */
    @PostMapping("/api/ai/chat/messages/{messageId}/feedback/")
    public ResponseEntity<ChatMessageDTO> sendFeedback(@PathVariable("messageId") UUID messageId,
                                                       @RequestBody ChatFeedbackRequestDTO request) {
        return ResponseEntity.ok(aiChatService.sendFeedback(SecurityUtil.getCurrentUserId(), messageId, request));
    }
}
