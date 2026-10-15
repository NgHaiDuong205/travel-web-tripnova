package com.duong.travelweb.model.dto;

import java.util.UUID;

/** DTO kết quả tạo tóm tắt AI. */
public class AiSummarizeResultDTO {

    private String status;
    private UUID jobId;
    private AiSummaryDTO summary;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public AiSummaryDTO getSummary() {
        return summary;
    }

    public void setSummary(AiSummaryDTO summary) {
        this.summary = summary;
    }
}
