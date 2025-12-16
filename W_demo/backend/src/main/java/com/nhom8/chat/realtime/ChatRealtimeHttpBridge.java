package com.nhom8.chat.realtime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.nhom8.chat.entity.ChatMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRealtimeHttpBridge {

    // inject bridge “thật” dùng chat-realtime
    private final ChatRealtimeBridge chatRealtimeBridge;
    


    public void broadcastMessage(ChatMessage msg, String clientCid) {
        chatRealtimeBridge.broadcastMessage(msg, clientCid);
    }

    public void notifyRead(Long messageId, Long userId) {
        chatRealtimeBridge.notifyRead(messageId, userId);
    }
      
    public void userJoinConversation(Long userId, Long conversationId) {
        chatRealtimeBridge.userJoinConversation(userId, conversationId);
    }

    public void userLeaveConversation(Long userId, Long conversationId) {
        chatRealtimeBridge.userLeaveConversation(userId, conversationId);
    }
}
