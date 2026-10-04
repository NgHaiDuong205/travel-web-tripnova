package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
public class ChatSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_token", length = 64)
    private String sessionToken;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "channel", length = 20)
    private String channel;

    @Column(name = "context_type", length = 30)
    private String contextType;

    @Column(name = "context_id")
    private UUID contextId;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() {
            return id;
        }

    public void setId(UUID id) {
            this.id = id;
        }

    public UUID getUserId() {
            return userId;
        }

    public void setUserId(UUID userId) {
            this.userId = userId;
        }

    public String getSessionToken() {
            return sessionToken;
        }

    public void setSessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
        }

    public String getTitle() {
            return title;
        }

    public void setTitle(String title) {
            this.title = title;
        }

    public String getChannel() {
            return channel;
        }

    public void setChannel(String channel) {
            this.channel = channel;
        }

    public String getContextType() {
            return contextType;
        }

    public void setContextType(String contextType) {
            this.contextType = contextType;
        }

    public UUID getContextId() {
            return contextId;
        }

    public void setContextId(UUID contextId) {
            this.contextId = contextId;
        }

    public Integer getTotalTokens() {
            return totalTokens;
        }

    public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

    public Boolean getIsActive() {
            return isActive;
        }

    public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }

    public LocalDateTime getCreatedAt() {
            return createdAt;
        }

    public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

    public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

    public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

}
