package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.ChatDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.ChatFeedbackRequestDTO;
import com.duong.travelweb.model.dto.ChatMessageDTO;
import com.duong.travelweb.model.dto.ChatReplyDTO;
import com.duong.travelweb.model.dto.ChatSendRequestDTO;
import com.duong.travelweb.model.dto.ChatSessionCreateRequestDTO;
import com.duong.travelweb.model.dto.ChatSessionDTO;
import com.duong.travelweb.model.entity.ChatFeedbackEntity;
import com.duong.travelweb.model.entity.ChatMessageEntity;
import com.duong.travelweb.model.entity.ChatSessionEntity;
import com.duong.travelweb.repository.ChatFeedbackRepository;
import com.duong.travelweb.repository.ChatMessageRepository;
import com.duong.travelweb.repository.ChatSessionRepository;
import com.duong.travelweb.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);
    private static final Set<String> CONTEXT_TYPES = Set.of("hotel", "tour", "car", "flight", "destination", "landmark");
    private static final int MAX_QUESTION_CHARS = 2000;
    private static final int MAX_COMMENT_CHARS = 1000;
    private static final int TITLE_CHARS = 80;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatFeedbackRepository feedbackRepository;
    private final AiServiceClient aiClient;
    private final ChatDTOConverter converter;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final int historyTurns;
    private final int rateLimitMax;
    private final int rateLimitWindowMinutes;

    public AiChatServiceImpl(ChatSessionRepository sessionRepository,
                             ChatMessageRepository messageRepository,
                             ChatFeedbackRepository feedbackRepository,
                             AiServiceClient aiClient,
                             ChatDTOConverter converter,
                             ObjectMapper objectMapper,
                             PlatformTransactionManager transactionManager,
                             @Value("${app.ai.history-turns:6}") int historyTurns,
                             @Value("${app.ai.rate-limit.max:20}") int rateLimitMax,
                             @Value("${app.ai.rate-limit.window-minutes:10}") int rateLimitWindowMinutes) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.feedbackRepository = feedbackRepository;
        this.aiClient = aiClient;
        this.converter = converter;
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(transactionManager);
        this.historyTurns = historyTurns;
        this.rateLimitMax = rateLimitMax;
        this.rateLimitWindowMinutes = rateLimitWindowMinutes;
    }

    @Override
    @Transactional
    public ChatSessionDTO createSession(UUID userId, ChatSessionCreateRequestDTO request) {
        String contextType = request == null ? null : trimToNull(request.getContextType());
        UUID contextId = request == null ? null : request.getContextId();
        if ((contextType == null) != (contextId == null)) {
            throw ApiException.badRequest("contextType và contextId phải đi cùng nhau");
        }
        if (contextType != null && !CONTEXT_TYPES.contains(contextType)) {
            throw ApiException.badRequest("contextType phải thuộc: " + String.join(", ", CONTEXT_TYPES));
        }
        LocalDateTime now = LocalDateTime.now();
        ChatSessionEntity session = new ChatSessionEntity();
        session.setUserId(userId);
        session.setChannel("web");
        session.setContextType(contextType);
        session.setContextId(contextId);
        session.setTotalTokens(0);
        session.setIsActive(true);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return converter.toSessionDTO(sessionRepository.save(session), 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatSessionDTO> findSessions(UUID userId, int page, int limit) {
        Page<ChatSessionEntity> result = sessionRepository.findActiveByUserId(userId, PageRequest.of(Math.max(page, 1) - 1, limit));
        Map<UUID, Long> counts = countMessages(result.getContent().stream().map(ChatSessionEntity::getId).toList());
        return result.map(s -> converter.toSessionDTO(s, counts.getOrDefault(s.getId(), 0L)));
    }

    @Override
    @Transactional
    public void closeSession(UUID userId, UUID sessionId) {
        ChatSessionEntity session = ownedActiveSession(userId, sessionId);
        session.setIsActive(false);
        session.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getMessages(UUID userId, UUID sessionId) {
        ownedActiveSession(userId, sessionId);
        List<ChatMessageEntity> messages = messageRepository.findBySessionId(sessionId);
        Map<UUID, ChatFeedbackEntity> feedback = feedbackOf(userId, messages);
        return messages.stream().map(m -> converter.toMessageDTO(m, feedback.get(m.getId()))).toList();
    }

    /**
     * Không chạy trong 1 transaction: lời gọi AI mất vài giây, không giữ kết nối DB trong lúc chờ.
     * B1 (tx): kiểm tra phiên + tần suất, lấy lịch sử, lưu câu hỏi. B2: gọi AI. B3 (tx): lưu câu trả lời.
     */
    @Override
    public ChatReplyDTO sendMessage(UUID userId, UUID sessionId, ChatSendRequestDTO request) {
        String content = request == null ? null : trimToNull(request.getContent());
        if (content == null) {
            throw ApiException.badRequest("Nội dung câu hỏi không được để trống");
        }
        if (content.length() > MAX_QUESTION_CHARS) {
            throw ApiException.badRequest("Câu hỏi tối đa " + MAX_QUESTION_CHARS + " ký tự");
        }
        String locale = "en".equalsIgnoreCase(request.getLocale()) ? "en" : "vi";
        // Ngữ cảnh trang đang xem (widget nổi trên mọi trang) ghi đè ngữ cảnh lúc tạo phiên; loại lạ thì bỏ qua.
        String pageType = trimToNull(request.getContextType());
        Map<String, Object> pageContext = null;
        if (pageType != null && CONTEXT_TYPES.contains(pageType) && request.getContextId() != null) {
            pageContext = new LinkedHashMap<>();
            pageContext.put("type", pageType);
            pageContext.put("id", request.getContextId().toString());
        }
        Map<String, Object> overrideContext = pageContext;

        Prepared prepared = tx.execute(status -> prepare(userId, sessionId, content, overrideContext));

        JsonNode result = null;
        String errorCode = null;
        long started = System.currentTimeMillis();
        try {
            result = aiClient.chat(content, prepared.history, locale, prepared.context);
        } catch (AiServiceClient.AiException ex) {
            errorCode = ex.getCode();
            log.warn("AI chat failed for session {}: {} ({})", sessionId, ex.getCode(), ex.getMessage());
        }
        JsonNode answer = result;
        String error = errorCode != null ? errorCode : (answer == null ? AiServiceClient.ERR_UNAVAILABLE : null);
        int latency = (int) (System.currentTimeMillis() - started);

        return tx.execute(status -> saveAnswer(userId, sessionId, prepared.userMessage, answer, error, locale, latency));
    }

    private Prepared prepare(UUID userId, UUID sessionId, String content, Map<String, Object> overrideContext) {
        ChatSessionEntity session = ownedActiveSession(userId, sessionId);
        LocalDateTime now = LocalDateTime.now();
        long recent = messageRepository.countUserQuestionsSince(userId, now.minusMinutes(rateLimitWindowMinutes));
        if (recent >= rateLimitMax) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã hỏi " + rateLimitMax + " câu trong " + rateLimitWindowMinutes + " phút, vui lòng thử lại sau ít phút");
        }

        // Lịch sử: N lượt gần nhất (bỏ tin lỗi), cũ trước.
        List<ChatMessageEntity> recentMessages = new ArrayList<>(messageRepository.findRecent(sessionId, historyTurns * 2));
        Collections.reverse(recentMessages);
        List<Map<String, String>> history = new ArrayList<>();
        for (ChatMessageEntity m : recentMessages) {
            if (m.getErrorMessage() == null && ("user".equals(m.getRole()) || "assistant".equals(m.getRole()))) {
                history.add(Map.of("role", m.getRole(), "content", m.getContent() == null ? "" : m.getContent()));
            }
        }

        ChatMessageEntity question = new ChatMessageEntity();
        question.setSessionId(sessionId);
        question.setRole("user");
        question.setContent(content);
        question.setCreatedAt(now);
        question = messageRepository.save(question);

        if (session.getTitle() == null) {
            session.setTitle(content.length() > TITLE_CHARS ? content.substring(0, TITLE_CHARS - 1) + "…" : content);
        }
        session.setUpdatedAt(now);

        Map<String, Object> context = overrideContext;
        if (context == null && session.getContextType() != null && session.getContextId() != null) {
            context = new LinkedHashMap<>();
            context.put("type", session.getContextType());
            context.put("id", session.getContextId().toString());
        }
        return new Prepared(question, history, context);
    }

    private ChatReplyDTO saveAnswer(UUID userId, UUID sessionId, ChatMessageEntity question, JsonNode result,
                                   String error, String locale, int fallbackLatency) {
        ChatSessionEntity session = ownedActiveSession(userId, sessionId);
        ChatMessageEntity answer = new ChatMessageEntity();
        answer.setSessionId(sessionId);
        answer.setRole("assistant");
        // Luôn sau câu hỏi kể cả khi đồng hồ lệch (thứ tự hiển thị theo created_at).
        LocalDateTime now = LocalDateTime.now();
        answer.setCreatedAt(now.isAfter(question.getCreatedAt()) ? now : question.getCreatedAt().plusNanos(1000));
        if (error != null || result == null) {
            answer.setContent(errorText(error, locale));
            answer.setErrorMessage(error == null ? AiServiceClient.ERR_UNAVAILABLE : error);
            answer.setLatencyMs(fallbackLatency);
        } else {
            answer.setContent(text(result, "answer"));
            answer.setRetrievedChunks(result.has("sources") ? result.get("sources").toString() : "[]");
            ObjectNode tool = objectMapper.createObjectNode();
            tool.put("sql", text(result, "sql"));
            tool.put("rowCount", result.path("rowCount").asInt(0));
            tool.put("sqlFailed", result.path("sqlFailed").asBoolean(false));
            answer.setToolCalls(tool.toString());
            String model = text(result, "model");
            answer.setModelName(model == null ? null : model.substring(0, Math.min(model.length(), 50)));
            answer.setPromptTokens(result.path("promptTokens").asInt(0));
            answer.setCompletionTokens(result.path("completionTokens").asInt(0));
            answer.setLatencyMs(result.hasNonNull("latencyMs") ? result.get("latencyMs").asInt() : fallbackLatency);
            int tokens = answer.getPromptTokens() + answer.getCompletionTokens();
            session.setTotalTokens((session.getTotalTokens() == null ? 0 : session.getTotalTokens()) + tokens);
        }
        answer = messageRepository.save(answer);
        session.setUpdatedAt(answer.getCreatedAt());

        ChatReplyDTO reply = new ChatReplyDTO();
        reply.setUserMessage(converter.toMessageDTO(question, null));
        reply.setAssistantMessage(converter.toMessageDTO(answer, null));
        long count = countMessages(List.of(sessionId)).getOrDefault(sessionId, 0L);
        reply.setSession(converter.toSessionDTO(session, count));
        return reply;
    }

    @Override
    @Transactional
    public ChatMessageDTO sendFeedback(UUID userId, UUID messageId, ChatFeedbackRequestDTO request) {
        if (request == null || (request.getRating() == null && request.getIsHelpful() == null)) {
            throw ApiException.badRequest("Cần rating (1-5) hoặc isHelpful");
        }
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw ApiException.badRequest("rating phải từ 1 đến 5");
        }
        String comment = trimToNull(request.getComment());
        if (comment != null && comment.length() > MAX_COMMENT_CHARS) {
            throw ApiException.badRequest("Nhận xét tối đa " + MAX_COMMENT_CHARS + " ký tự");
        }
        ChatMessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tin nhắn"));
        ownedActiveSession(userId, message.getSessionId());
        if (!"assistant".equals(message.getRole()) || message.getErrorMessage() != null) {
            throw ApiException.badRequest("Chỉ đánh giá được câu trả lời của trợ lý");
        }
        feedbackRepository.upsert(messageId, userId, request.getRating(), request.getIsHelpful(), comment);
        ChatFeedbackEntity saved = feedbackRepository.findByUserAndMessages(userId, List.of(messageId))
                .stream().findFirst().orElse(null);
        return converter.toMessageDTO(message, saved);
    }

    // ------------------------------------------------------------------ helpers

    private ChatSessionEntity ownedActiveSession(UUID userId, UUID sessionId) {
        ChatSessionEntity session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiên chat"));
        if (!Boolean.TRUE.equals(session.getIsActive())) {
            throw ApiException.notFound("Không tìm thấy phiên chat");
        }
        return session;
    }

    private Map<UUID, Long> countMessages(List<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : messageRepository.countBySessionIds(sessionIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<UUID, ChatFeedbackEntity> feedbackOf(UUID userId, List<ChatMessageEntity> messages) {
        List<UUID> ids = messages.stream().filter(m -> "assistant".equals(m.getRole())).map(ChatMessageEntity::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return feedbackRepository.findByUserAndMessages(userId, ids).stream()
                .collect(Collectors.toMap(ChatFeedbackEntity::getMessageId, Function.identity(), (a, b) -> a));
    }

    private static String errorText(String code, String locale) {
        boolean en = "en".equals(locale);
        if (AiServiceClient.ERR_QUOTA.equals(code)) {
            return en ? "The assistant is busy right now (usage limit reached). Please try again in a few minutes."
                    : "Trợ lý đang quá tải (tạm hết hạn mức). Bạn vui lòng thử lại sau ít phút.";
        }
        return en ? "Sorry, the assistant could not answer right now. Please try again."
                : "Xin lỗi, trợ lý chưa trả lời được lúc này. Bạn vui lòng thử lại.";
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asString() : null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Prepared(ChatMessageEntity userMessage, List<Map<String, String>> history, Map<String, Object> context) {
    }
}
