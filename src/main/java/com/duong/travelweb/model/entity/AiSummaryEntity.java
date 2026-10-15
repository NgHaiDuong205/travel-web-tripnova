package com.duong.travelweb.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** Thực thể lưu tóm tắt AI. */
@Entity
@Table(name = "ai_summaries")
public class AiSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "entity_type", length = 20)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "summary_type", length = 30)
    private String summaryType;

    @Column(name = "language", columnDefinition = "bpchar", length = 2)
    private String language;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "based_on_count")
    private Integer basedOnCount;

    @Column(name = "model_version", length = 30)
    private String modelVersion;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getBasedOnCount() {
        return basedOnCount;
    }

    public void setBasedOnCount(Integer basedOnCount) {
        this.basedOnCount = basedOnCount;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
