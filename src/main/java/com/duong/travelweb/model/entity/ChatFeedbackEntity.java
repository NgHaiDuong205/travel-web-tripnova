package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_feedback")
public class ChatFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "rating")
    private Short rating;

    @Column(name = "is_helpful")
    private Boolean isHelpful;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() {
            return id;
        }

    public void setId(UUID id) {
            this.id = id;
        }

    public UUID getMessageId() {
            return messageId;
        }

    public void setMessageId(UUID messageId) {
            this.messageId = messageId;
        }

    public UUID getUserId() {
            return userId;
        }

    public void setUserId(UUID userId) {
            this.userId = userId;
        }

    public Short getRating() {
            return rating;
        }

    public void setRating(Short rating) {
            this.rating = rating;
        }

    public Boolean getIsHelpful() {
            return isHelpful;
        }

    public void setIsHelpful(Boolean isHelpful) {
            this.isHelpful = isHelpful;
        }

    public String getComment() {
            return comment;
        }

    public void setComment(String comment) {
            this.comment = comment;
        }

    public LocalDateTime getCreatedAt() {
            return createdAt;
        }

    public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

}
