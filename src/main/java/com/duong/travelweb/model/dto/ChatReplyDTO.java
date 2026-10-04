package com.duong.travelweb.model.dto;

public class ChatReplyDTO {
    private ChatMessageDTO userMessage;

    private ChatMessageDTO assistantMessage;

    private ChatSessionDTO session;

    public ChatMessageDTO getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(ChatMessageDTO userMessage) {
        this.userMessage = userMessage;
    }

    public ChatMessageDTO getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(ChatMessageDTO assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public ChatSessionDTO getSession() {
        return session;
    }

    public void setSession(ChatSessionDTO session) {
        this.session = session;
    }
}
