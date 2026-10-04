package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.AdminChatMessageDTO;
import com.duong.travelweb.model.dto.ChatMessageDTO;
import com.duong.travelweb.model.dto.ChatSessionDTO;
import com.duong.travelweb.model.dto.ChatSourceDTO;
import com.duong.travelweb.model.dto.KnowledgeDocumentDTO;
import com.duong.travelweb.model.entity.ChatFeedbackEntity;
import com.duong.travelweb.model.entity.ChatMessageEntity;
import com.duong.travelweb.model.entity.ChatSessionEntity;
import com.duong.travelweb.model.entity.KnowledgeDocumentEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Map chat / tài liệu RAG sang DTO; đọc các cột jsonb (retrieved_chunks, tool_calls) lưu dạng chuỗi. */
@Component
public class ChatDTOConverter {
    private final ObjectMapper objectMapper;

    public ChatDTOConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChatSessionDTO toSessionDTO(ChatSessionEntity entity, Long messageCount) {
        ChatSessionDTO dto = new ChatSessionDTO();
        fillSession(dto, entity, messageCount);
        return dto;
    }

    public void fillSession(ChatSessionDTO dto, ChatSessionEntity entity, Long messageCount) {
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContextType(entity.getContextType());
        dto.setContextId(entity.getContextId());
        dto.setTotalTokens(entity.getTotalTokens());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setMessageCount(messageCount);
    }

    public ChatMessageDTO toMessageDTO(ChatMessageEntity entity, ChatFeedbackEntity feedback) {
        ChatMessageDTO dto = new ChatMessageDTO();
        fillMessage(dto, entity, feedback);
        return dto;
    }

    public AdminChatMessageDTO toAdminMessageDTO(ChatMessageEntity entity, ChatFeedbackEntity feedback) {
        AdminChatMessageDTO dto = new AdminChatMessageDTO();
        fillMessage(dto, entity, feedback);
        JsonNode tool = parse(entity.getToolCalls());
        if (tool != null && tool.isObject()) {
            dto.setSql(text(tool, "sql"));
            dto.setRowCount(tool.hasNonNull("rowCount") ? tool.get("rowCount").asInt() : null);
            dto.setSqlFailed(tool.hasNonNull("sqlFailed") ? tool.get("sqlFailed").asBoolean() : null);
        }
        dto.setModelName(entity.getModelName());
        dto.setPromptTokens(entity.getPromptTokens());
        dto.setCompletionTokens(entity.getCompletionTokens());
        dto.setLatencyMs(entity.getLatencyMs());
        dto.setErrorMessage(entity.getErrorMessage());
        return dto;
    }

    private void fillMessage(ChatMessageDTO dto, ChatMessageEntity entity, ChatFeedbackEntity feedback) {
        dto.setId(entity.getId());
        dto.setRole(entity.getRole());
        dto.setContent(entity.getContent());
        dto.setIsError(entity.getErrorMessage() != null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setSources(toSources(entity.getRetrievedChunks()));
        if (feedback != null) {
            dto.setFeedbackRating(feedback.getRating());
            dto.setFeedbackHelpful(feedback.getIsHelpful());
        }
    }

    private List<ChatSourceDTO> toSources(String json) {
        List<ChatSourceDTO> out = new ArrayList<>();
        JsonNode array = parse(json);
        if (array == null || !array.isArray()) {
            return out;
        }
        for (JsonNode node : array) {
            ChatSourceDTO source = new ChatSourceDTO();
            source.setCitation(text(node, "citation"));
            String documentId = text(node, "documentId");
            source.setDocumentId(documentId == null ? null : UUID.fromString(documentId));
            source.setTitle(text(node, "title"));
            source.setChunkIndex(node.hasNonNull("chunkIndex") ? node.get("chunkIndex").asInt() : null);
            source.setText(text(node, "text"));
            source.setScore(node.hasNonNull("score") ? node.get("score").asDouble() : null);
            out.add(source);
        }
        return out;
    }

    public KnowledgeDocumentDTO toDocumentDTO(KnowledgeDocumentEntity entity, Long chunkCount) {
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
        dto.setId(entity.getId());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceId(entity.getSourceId());
        dto.setTitle(entity.getTitle());
        dto.setUrl(entity.getUrl());
        dto.setLanguage(entity.getLanguage() == null ? null : entity.getLanguage().trim());
        dto.setVersion(entity.getVersion());
        dto.setIsActive(entity.getIsActive());
        dto.setIndexedAt(entity.getIndexedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setEmbeddingModel(entity.getEmbeddingModel());
        dto.setChunkCount(chunkCount == null ? 0L : chunkCount);
        dto.setContentLength(entity.getContent() == null ? 0 : entity.getContent().length());
        return dto;
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asString() : null;
    }
}
