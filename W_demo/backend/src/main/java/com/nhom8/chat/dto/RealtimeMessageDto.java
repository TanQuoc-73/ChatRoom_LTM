package com.nhom8.chat.dto;
import java.time.Instant;

import lombok.Data;
@Data
public class RealtimeMessageDto {
        private String messageId;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String content;
    private String messageType;
    private String clientCid;
    private Instant sentAt;
    private boolean isEdited;
    private boolean isDeleted;
}



