package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.ChatDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AdminChatMessageDTO;
import com.duong.travelweb.model.dto.AdminChatSessionDTO;
import com.duong.travelweb.model.dto.KnowledgeDocumentDTO;
import com.duong.travelweb.model.entity.ChatFeedbackEntity;
import com.duong.travelweb.model.entity.ChatMessageEntity;
import com.duong.travelweb.model.entity.ChatSessionEntity;
import com.duong.travelweb.model.entity.KnowledgeDocumentEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.ChatFeedbackRepository;
import com.duong.travelweb.repository.ChatMessageRepository;
import com.duong.travelweb.repository.ChatSessionRepository;
import com.duong.travelweb.repository.KnowledgeDocumentRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.AdminAiService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminAiServiceImpl implements AdminAiService {
    private static final Set<String> SOURCE_TYPES = Set.of("policy", "faq", "guide", "hotel", "tour", "car", "flight",
            "destination", "other");
    private static final Set<String> EXTENSIONS = Set.of("txt", "md", "pdf");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final KnowledgeDocumentRepository documentRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiClient;
    private final ChatDTOConverter converter;

    public AdminAiServiceImpl(KnowledgeDocumentRepository documentRepository,
                              ChatSessionRepository sessionRepository,
                              ChatMessageRepository messageRepository,
                              ChatFeedbackRepository feedbackRepository,
                              UserRepository userRepository,
                              AiServiceClient aiClient,
                              ChatDTOConverter converter) {
        this.documentRepository = documentRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
        this.converter = converter;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeDocumentDTO> searchDocuments(String q, String sourceType, String language, Boolean active,
                                                      int page, int limit) {
        Page<KnowledgeDocumentEntity> result = documentRepository.search(blankToNull(q), blankToNull(sourceType),
                blankToNull(language), active, PageRequest.of(Math.max(page, 1) - 1, limit));
        Map<UUID, Long> chunks = chunkCounts(result.getContent().stream().map(KnowledgeDocumentEntity::getId).toList());
        return result.map(d -> converter.toDocumentDTO(d, chunks.get(d.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocumentDTO getDocument(UUID id) {
        KnowledgeDocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài liệu"));
        return converter.toDocumentDTO(doc, chunkCounts(List.of(id)).get(id));
    }

    @Override
    public KnowledgeDocumentDTO uploadDocument(MultipartFile file, String title, String sourceType, UUID sourceId,
                                               String language, String url) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Chưa chọn tệp");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Tệp tối đa 5 MB");
        }
        String filename = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()
                .replace('\\', '/').replaceAll(".*/", "");
        int dot = filename.lastIndexOf('.');
        String ext = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(ext)) {
            throw ApiException.badRequest("Chỉ nhận tệp .txt, .md, .pdf");
        }
        String type = blankToNull(sourceType) == null ? "policy" : sourceType.trim();
        if (!SOURCE_TYPES.contains(type)) {
            throw ApiException.badRequest("sourceType phải thuộc: " + String.join(", ", SOURCE_TYPES));
        }
        String lang = blankToNull(language) == null ? "vi" : language.trim();
        if (!"vi".equals(lang) && !"en".equals(lang)) {
            throw ApiException.badRequest("language phải là vi hoặc en");
        }
        String cleanTitle = blankToNull(title);
        if (cleanTitle != null && cleanTitle.length() > 255) {
            throw ApiException.badRequest("Tiêu đề tối đa 255 ký tự");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw ApiException.badRequest("Không đọc được tệp");
        }
        JsonNode result = aiClient.uploadDocument(filename, bytes, cleanTitle, type, sourceId, lang, blankToNull(url));
        if (result == null || !result.hasNonNull("id")) {
            throw new AiServiceClient.AiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    AiServiceClient.ERR_UNAVAILABLE, "Dịch vụ AI không trả về tài liệu");
        }
        KnowledgeDocumentDTO dto = getDocument(UUID.fromString(result.get("id").asString()));
        return dto;
    }

    @Override
    public KnowledgeDocumentDTO setDocumentActive(UUID id, boolean active) {
        // Không đọc lại sau khi Python cập nhật: open-in-view giữ entity cũ trong persistence context của request.
        KnowledgeDocumentDTO dto = getDocument(id);
        aiClient.setDocumentActive(id, active);
        dto.setIsActive(active);
        return dto;
    }

    @Override
    public void deleteDocument(UUID id) {
        getDocument(id);
        aiClient.deleteDocument(id);
    }

    @Override
    public Map<String, Object> reindex(boolean full) {
        JsonNode result = aiClient.reindex(full);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("documents", result == null ? 0 : result.path("documents").asInt(0));
        out.put("chunks", result == null ? 0 : result.path("chunks").asInt(0));
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminChatSessionDTO> searchSessions(UUID userId, String q, LocalDate from, LocalDate to,
                                                    int page, int limit) {
        Page<ChatSessionEntity> result = sessionRepository.adminSearch(userId, blankToNull(q),
                from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(page, 1) - 1, limit));
        List<UUID> ids = result.getContent().stream().map(ChatSessionEntity::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : messageRepository.countBySessionIds(ids)) {
                counts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }
        Map<UUID, UserEntity> users = userRepository.findAllById(result.getContent().stream()
                        .map(ChatSessionEntity::getUserId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return result.map(s -> {
            AdminChatSessionDTO dto = new AdminChatSessionDTO();
            converter.fillSession(dto, s, counts.getOrDefault(s.getId(), 0L));
            dto.setUserId(s.getUserId());
            UserEntity user = s.getUserId() == null ? null : users.get(s.getUserId());
            if (user != null) {
                dto.setUserEmail(user.getEmail());
                dto.setUserFullName(user.getFullName());
            }
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminChatMessageDTO> getSessionMessages(UUID sessionId) {
        ChatSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiên chat"));
        List<ChatMessageEntity> messages = messageRepository.findBySessionId(sessionId);
        List<UUID> ids = messages.stream().map(ChatMessageEntity::getId).toList();
        // Đánh giá do chính chủ phiên gửi.
        Map<UUID, ChatFeedbackEntity> feedback = ids.isEmpty() ? Map.of() : feedbackRepository.findByMessages(ids).stream()
                .filter(f -> session.getUserId() != null && session.getUserId().equals(f.getUserId()))
                .collect(Collectors.toMap(ChatFeedbackEntity::getMessageId, Function.identity(), (a, b) -> a));
        return messages.stream().map(m -> converter.toAdminMessageDTO(m, feedback.get(m.getId()))).toList();
    }

    private Map<UUID, Long> chunkCounts(List<UUID> ids) {
        Map<UUID, Long> counts = new HashMap<>();
        if (ids.isEmpty()) {
            return counts;
        }
        for (Object[] row : documentRepository.countChunks(ids)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
