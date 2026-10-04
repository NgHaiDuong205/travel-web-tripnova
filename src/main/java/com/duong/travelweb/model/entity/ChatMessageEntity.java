package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "role", length = 20)
    private String role;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "tool_calls", columnDefinition = "jsonb")
    private String toolCalls;

    @Column(name = "retrieved_chunks", columnDefinition = "jsonb")
    private String retrievedChunks;

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() {
            return id;
        }

    public void setId(UUID id) {
            this.id = id;
        }

    public UUID getSessionId() {
            return sessionId;
        }

    public void setSessionId(UUID sessionId) {
            this.sessionId = sessionId;
        }

    public String getRole() {
            return role;
        }

    public void setRole(String role) {
            this.role = role;
        }

    public String getContent() {
            return content;
        }

    public void setContent(String content) {
            this.content = content;
        }

    public String getToolCalls() {
            return toolCalls;
        }

    public void setToolCalls(String toolCalls) {
            this.toolCalls = toolCalls;
        }

    public String getRetrievedChunks() {
            return retrievedChunks;
        }

    public void setRetrievedChunks(String retrievedChunks) {
            this.retrievedChunks = retrievedChunks;
        }

    public String getModelName() {
            return modelName;
        }

    public void setModelName(String modelName) {
            this.modelName = modelName;
        }

    public Integer getPromptTokens() {
            return promptTokens;
        }

    public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

    public Integer getCompletionTokens() {
            return completionTokens;
        }

    public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

    public Integer getLatencyMs() {
            return latencyMs;
        }

    public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
        }

    public String getErrorMessage() {
            return errorMessage;
        }

    public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

    public LocalDateTime getCreatedAt() {
            return createdAt;
        }

    public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

}
