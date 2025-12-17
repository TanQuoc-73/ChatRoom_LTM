package com.nhom8.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FriendOnlineDTO {
    private Long userId;
    private String displayName;
    private String avatarUrl;
    private boolean online;
}
