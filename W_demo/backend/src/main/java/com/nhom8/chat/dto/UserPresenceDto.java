package com.nhom8.chat.dto;

import java.time.Instant;

import lombok.Data;


@Data
public class UserPresenceDto {
        private String userId;
    private String username;
    private String displayName;
    private boolean online;
    private Instant lastSeen;
    private String currentConversationId;
}
