package com.nhom8.chat.dto;

import com.nhom8.chat.entity.enums.MessageKind;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private MessageKind messageType;   
    private Instant sentAt;
    private List<Long> attachmentIds;
    private String clientCid;
    private boolean isEdited;
}
