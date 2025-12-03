package com.nhom8.chat.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ConversationCreateRequest {
    @NotBlank private String type; 
    private String name;
    private String description;
    private Boolean isPublic = false;
    private Integer maxMembers;
    private List<Long> memberIds;
}
