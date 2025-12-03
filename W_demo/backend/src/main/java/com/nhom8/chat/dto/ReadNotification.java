package com.nhom8.chat.dto;

import lombok.Data;

@Data
public class ReadNotification {
    private Long conversationId;
    private Long userId;
    private Long messageId;
    private Long timestamp;
}