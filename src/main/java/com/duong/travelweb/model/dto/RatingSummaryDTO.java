package com.duong.travelweb.model.dto;

import java.util.Map;

/** Tổng hợp điểm đánh giá của một đối tượng (chỉ bài đã duyệt, có rating). */
public class RatingSummaryDTO {
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> distribution;

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Map<Integer, Long> getDistribution() {
        return distribution;
    }

    public void setDistribution(Map<Integer, Long> distribution) {
        this.distribution = distribution;
    }
}
