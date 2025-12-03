package com.nhom8.chat.mapper;

import com.nhom8.chat.dto.FriendshipDTO;
import com.nhom8.chat.entity.Friendship;

public class FriendshipMapper {
    
    public static FriendshipDTO toDTO(Friendship friendship) {
        if (friendship == null) return null;
        
        FriendshipDTO dto = new FriendshipDTO();
        dto.setId(friendship.getId());
        dto.setUser1Id(friendship.getUser1().getId());
        dto.setUser1Name(friendship.getUser1().getDisplayName());
        dto.setUser2Id(friendship.getUser2().getId());
        dto.setUser2Name(friendship.getUser2().getDisplayName());
        dto.setStatus(friendship.getStatus());
        dto.setActionUserId(friendship.getActionUser().getId());
        dto.setActionUserName(friendship.getActionUser().getDisplayName());
        dto.setCreatedAt(friendship.getCreatedAt());
        dto.setUpdatedAt(friendship.getUpdatedAt());
        
        return dto;
    }
}