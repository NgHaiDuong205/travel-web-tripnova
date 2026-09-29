package com.duong.travelweb.builder;

import java.util.UUID;

/** Bộ lọc danh sách bài viết. Trường null = bỏ qua. */
public class PostSearchBuilder {
    private final String entityType;
    private final UUID entityId;
    private final UUID userId;
    private final String status;
    private final String keyword;
    private final Integer rating;
    /** newest | oldest | top | rating_high | rating_low */
    private final String sort;

    private PostSearchBuilder(Builder builder) {
        this.entityType = builder.entityType;
        this.entityId = builder.entityId;
        this.userId = builder.userId;
        this.status = builder.status;
        this.keyword = builder.keyword;
        this.rating = builder.rating;
        this.sort = builder.sort;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public String getKeyword() {
        return keyword;
    }

    public Integer getRating() {
        return rating;
    }

    public String getSort() {
        return sort;
    }

    public static class Builder {
        private String entityType;
        private UUID entityId;
        private UUID userId;
        private String status;
        private String keyword;
        private Integer rating;
        private String sort;

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(UUID entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder rating(Integer rating) {
            this.rating = rating;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public PostSearchBuilder build() {
            return new PostSearchBuilder(this);
        }
    }
}
