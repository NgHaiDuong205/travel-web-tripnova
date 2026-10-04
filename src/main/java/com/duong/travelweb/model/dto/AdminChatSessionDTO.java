package com.duong.travelweb.model.dto;

import java.util.UUID;

public class AdminChatSessionDTO extends ChatSessionDTO {
    private UUID userId;

    private String userEmail;

    private String userFullName;

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

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }
}
