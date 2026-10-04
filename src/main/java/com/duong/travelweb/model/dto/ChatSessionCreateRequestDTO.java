package com.duong.travelweb.model.dto;

import java.util.UUID;

public class ChatSessionCreateRequestDTO {
    private String contextType;

    private UUID contextId;

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public UUID getContextId() {
        return contextId;
    }

    public void setContextId(UUID contextId) {
        this.contextId = contextId;
    }
}
