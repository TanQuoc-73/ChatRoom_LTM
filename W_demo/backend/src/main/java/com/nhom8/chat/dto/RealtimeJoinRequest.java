package com.nhom8.chat.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RealtimeJoinRequest {
    @NotBlank
    private String conversationId;
    
    private String sessionToken; // Optional for authentication
    
}
