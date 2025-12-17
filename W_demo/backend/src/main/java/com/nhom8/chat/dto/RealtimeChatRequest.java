package com.nhom8.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RealtimeChatRequest {
    @NotBlank
    private String conversationId; // Maps to roomId
    
    @NotBlank
    private String message;
    
    private String clientCid; // Client-generated message ID
    private String messageType = "TEXT"; // TEXT, IMAGE, FILE, etc.
}



