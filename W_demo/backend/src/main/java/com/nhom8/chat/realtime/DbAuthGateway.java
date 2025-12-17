package com.nhom8.chat.realtime;

import chat.core.spi.AuthGateway;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.service.ConversationService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DbAuthGateway implements AuthGateway {

    private final AppUserRepository userRepo;
    private final ConversationService conversationService;

    @Override
public boolean authenticate(String username, String password) {
    // Allow system internal user
    if ("SYSTEM".equalsIgnoreCase(username)) {
        return true;
    }

    Long userId = parseUserId(username);
    if (userId == null) {
        return false;
    }

    return userRepo.findById(userId).isPresent();
}
    @Override
    public boolean isUserInRoom(String username, String roomId) {
        Long userId = parseUserId(username);
        Long convId = parseConvId(roomId);
        return conversationService.isUserInConversation(convId, userId);
    }

    @Override
    public boolean canUserJoinRoom(String username, String roomId) {
        // tuỳ bạn muốn chặt tới đâu:
        // đơn giản nhất: nếu room tồn tại & user là member thì cho phép
        Long userId = parseUserId(username);
        Long convId = parseConvId(roomId);
        return conversationService.isUserInConversation(convId, userId);
        // Nếu muốn cho join khi là bạn bè, có thể dùng friendshipService ở ConversationService
    }

private Long parseUserId(String username) {
    // Accept numeric usernames and SYSTEM
    if ("SYSTEM".equalsIgnoreCase(username)) {
        return 0L;
    }

    try {
        return Long.parseLong(username);
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid username: " + username);
    }
}

    private Long parseConvId(String roomId) {
        if (!roomId.startsWith("conv_")) {
            throw new IllegalArgumentException("Invalid roomId: " + roomId);
        }
        return Long.parseLong(roomId.substring(5));
    }
}
