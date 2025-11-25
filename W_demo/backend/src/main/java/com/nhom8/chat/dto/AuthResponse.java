package com.nhom8.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private String displayName;
    private String sessionToken;

    public static AuthResponse success(String message, Long userId, String username, String displayName,
            String sessionToken) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .userId(userId)
                .username(username)
                .displayName(displayName)
                .sessionToken(sessionToken)
                .build();
    }

    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}