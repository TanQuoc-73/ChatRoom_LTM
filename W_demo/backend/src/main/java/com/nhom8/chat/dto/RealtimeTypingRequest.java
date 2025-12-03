package com.nhom8.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RealtimeTypingRequest {
    @NotBlank
    private String conversationId;
    
    private boolean typing = true; // true = typing, false = stopped
}
