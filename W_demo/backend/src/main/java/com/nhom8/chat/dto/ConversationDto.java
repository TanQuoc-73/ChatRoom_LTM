
package com.nhom8.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
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
