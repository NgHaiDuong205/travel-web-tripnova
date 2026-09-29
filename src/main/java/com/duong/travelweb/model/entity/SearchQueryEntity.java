package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_queries")
public class SearchQueryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "query_text")
    private String queryText;

    @Column(name = "filters")
    private String filters;

    @Column(name = "result_count")
    private Integer resultCount;

    @Column(name = "clicked_entity_type")
    private String clickedEntityType;

    @Column(name = "clicked_entity_id")
    private UUID clickedEntityId;

    @Column(name = "created_at", insertable = false, updatable = false)
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
