package com.nhom8.chat.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data @NoArgsConstructor @AllArgsConstructor
public class ConversationCreateRequest {
    @NotBlank private String type; 
    private String name;
    private String description;
    private Boolean isPublic = false;
    private Integer maxMembers;
}
