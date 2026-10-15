package com.duong.travelweb.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTO cho tóm tắt AI. */
public class AiSummaryDTO {

    private String entityType;
    private UUID entityId;
    private String summaryType;
    private String language;
    private String overview;
    private List<String> pros;
    private List<String> cons;
    private List<String> bestFor;
    private Integer basedOnCount;
    private Long currentReviewCount;
    private Boolean stale;
    private String modelVersion;
    private LocalDateTime generatedAt;
    private String entityName;

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getSummaryType() { return summaryType; }
    public void setSummaryType(String summaryType) { this.summaryType = summaryType; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public List<String> getPros() { return pros; }
    public void setPros(List<String> pros) { this.pros = pros; }
    public List<String> getCons() { return cons; }
    public void setCons(List<String> cons) { this.cons = cons; }
    public List<String> getBestFor() { return bestFor; }
    public void setBestFor(List<String> bestFor) { this.bestFor = bestFor; }
    public Integer getBasedOnCount() { return basedOnCount; }
    public void setBasedOnCount(Integer basedOnCount) { this.basedOnCount = basedOnCount; }
    public Long getCurrentReviewCount() { return currentReviewCount; }
    public void setCurrentReviewCount(Long currentReviewCount) { this.currentReviewCount = currentReviewCount; }
    public Boolean getStale() { return stale; }
    public void setStale(Boolean stale) { this.stale = stale; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
}
