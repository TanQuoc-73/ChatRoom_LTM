package com.nhom8.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberDto {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String role;
    private String nickname;
    private boolean muted;
}