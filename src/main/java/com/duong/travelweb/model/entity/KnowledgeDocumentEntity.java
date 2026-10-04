package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, insertable = false)
    private UUID id;

    @Column(name = "source_type", length = 30, insertable = false, updatable = false)
    private String sourceType;

    @Column(name = "source_id", insertable = false, updatable = false)
    private UUID sourceId;

    @Column(name = "title", length = 255, insertable = false, updatable = false)
    private String title;

    @Column(name = "content", columnDefinition = "text", insertable = false, updatable = false)
    private String content;

    @Column(name = "url", columnDefinition = "text", insertable = false, updatable = false)
    private String url;

    private String language;

    @Column(name = "version", insertable = false, updatable = false)
    private Integer version;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "indexed_at", insertable = false, updatable = false)
    private LocalDateTime indexedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String contentSha256;

    @Column(name = "embedding_model", length = 100, insertable = false, updatable = false)
    private String embeddingModel;

    public UUID getId() {
            return id;
        }

    public void setId(UUID id) {
            this.id = id;
        }

    public String getSourceType() {
            return sourceType;
        }

    public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

    public UUID getSourceId() {
            return sourceId;
        }

    public void setSourceId(UUID sourceId) {
            this.sourceId = sourceId;
        }

    public String getTitle() {
            return title;
        }

    public void setTitle(String title) {
            this.title = title;
        }

    public String getContent() {
            return content;
        }

    public void setContent(String content) {
            this.content = content;
        }

    public String getUrl() {
            return url;
        }

    public void setUrl(String url) {
            this.url = url;
        }

    public String getLanguage() {
            return language;
        }

    public void setLanguage(String language) {
            this.language = language;
        }

    public Integer getVersion() {
            return version;
        }

    public void setVersion(Integer version) {
            this.version = version;
        }

    public Boolean getIsActive() {
            return isActive;
        }

    public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }

    public LocalDateTime getIndexedAt() {
            return indexedAt;
        }

    public void setIndexedAt(LocalDateTime indexedAt) {
            this.indexedAt = indexedAt;
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

    public String getContentSha256() {
            return contentSha256;
        }

    public void setContentSha256(String contentSha256) {
            this.contentSha256 = contentSha256;
        }

    public String getEmbeddingModel() {
            return embeddingModel;
        }

    public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

}
