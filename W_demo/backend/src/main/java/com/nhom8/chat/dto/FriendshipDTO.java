package com.nhom8.chat.dto;

import java.time.Instant;

import com.nhom8.chat.entity.enums.FriendshipStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class FriendshipDTO {
    private Long id;
    private Long user1Id;
    private String user1Name;
    private Long user2Id;
    private String user2Name;
    private FriendshipStatus status;
    private Long actionUserId;
    private String actionUserName;
    private Instant createdAt;
    private Instant updatedAt;
    
}