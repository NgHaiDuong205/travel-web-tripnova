package com.duong.travelweb.model.dto;

import java.util.UUID;

public class ChatSendRequestDTO {
    private String content;

    private String locale;

    /** Ngữ cảnh trang khách đang xem (tuỳ chọn) — ghi đè ngữ cảnh của phiên cho câu hỏi này. */
    private String contextType;

    private UUID contextId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

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
