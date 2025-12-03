package com.nhom8.chat.dto;

import lombok.Data;

@Data
public class TypingNotification {
    private Long conversationId;
    private Long userId;
    private String username;
    private Boolean isTyping; 
    private Long timestamp;
}