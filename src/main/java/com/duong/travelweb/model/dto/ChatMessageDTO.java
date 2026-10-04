package com.duong.travelweb.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ChatMessageDTO {
    private UUID id;

    private String role;

    private String content;

    private List<ChatSourceDTO> sources;

    private Boolean isError;

    private LocalDateTime createdAt;

    private Short feedbackRating;

    private Boolean feedbackHelpful;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public List<ChatSourceDTO> getSources() {
        return sources;
    }

    public void setSources(List<ChatSourceDTO> sources) {
        this.sources = sources;
    }

    public Boolean getIsError() {
        return isError;
    }

    public void setIsError(Boolean isError) {
        this.isError = isError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Short getFeedbackRating() {
        return feedbackRating;
    }

    public void setFeedbackRating(Short feedbackRating) {
        this.feedbackRating = feedbackRating;
    }

    public Boolean getFeedbackHelpful() {
        return feedbackHelpful;
    }

    public void setFeedbackHelpful(Boolean feedbackHelpful) {
        this.feedbackHelpful = feedbackHelpful;
    }
}
