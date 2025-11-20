
package com.nhom8.chat.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationDto {
    private Long id;
    private String type;
    private String name;
    private String description;
    private Long createdBy;
    private boolean isPublic;
    private Integer maxMembers;
    private Long lastMessageId;
}
