package com.duong.travelweb.model.dto;

public class ChatSendRequestDTO {
    private String content;

    private String locale;

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
}
