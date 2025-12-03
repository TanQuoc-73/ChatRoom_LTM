package com.nhom8.chat.realtime;

import org.springframework.stereotype.Component;

import com.nhom8.chat.entity.ChatMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRealtimeHttpBridge {

    // inject bridge “thật” dùng chat-realtime
    private final ChatRealtimeBridge chatRealtimeBridge;

    // Service / controller backend vẫn gọi class này
    // nhưng bên trong nó sẽ gọi vào ChatRealtimeBridge (không còn HTTP)

    public void broadcastMessage(ChatMessage msg, String clientCid) {
        chatRealtimeBridge.broadcastMessage(msg, clientCid);
    }

    public void notifyRead(Long messageId, Long userId) {
        chatRealtimeBridge.notifyRead(messageId, userId);
    }
}
