package com.nhom8.chat.realtime;

import com.nhom8.chat.entity.ChatMessage;

public interface ChatRealtimeBridge {
    void broadcastMessage(ChatMessage msg, String clientCid);
    void notifyRead(Long messageId, Long userId);
}
