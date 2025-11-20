package com.nhom8.chat.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import com.nhom8.chat.entity.enums.MessageKind;

@Data @NoArgsConstructor @AllArgsConstructor
public class MessageRequest {
    @NotNull private Long conversationId;
    @NotNull @Size(min=1) private String content;
    private MessageKind messageType;
    private String clientCid; 
    private List<Long> attachmentMediaIds;
}
