package com.duong.travelweb.model.dto;

public class AdminChatMessageDTO extends ChatMessageDTO {
    private String sql;

    private Integer rowCount;

    private Boolean sqlFailed;

    private String modelName;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer latencyMs;

    private String errorMessage;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Boolean getSqlFailed() {
        return sqlFailed;
    }

    public void setSqlFailed(Boolean sqlFailed) {
        this.sqlFailed = sqlFailed;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
