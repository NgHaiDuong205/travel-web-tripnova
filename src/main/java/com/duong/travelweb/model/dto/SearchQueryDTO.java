package com.duong.travelweb.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class SearchQueryDTO {
    private Long id;

    private UUID userId;

    private String userEmail;

    private String queryText;

    private String filters;

    private Integer resultCount;

    private String clickedEntityType;

    private UUID clickedEntityId;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public String getClickedEntityType() {
        return clickedEntityType;
    }

    public void setClickedEntityType(String clickedEntityType) {
        this.clickedEntityType = clickedEntityType;
    }

    public UUID getClickedEntityId() {
        return clickedEntityId;
    }

    public void setClickedEntityId(UUID clickedEntityId) {
        this.clickedEntityId = clickedEntityId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
