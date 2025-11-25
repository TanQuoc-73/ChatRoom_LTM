package com.nhom8.chat.realtime;

import com.nhom8.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatRealtimeHttpBridge implements ChatRealtimeBridge {

    private final RestTemplate restTemplate; 
    private final String realtimeBase = "http://localhost:9000/api/realtime"; 

    @Override
    public void broadcastMessage(ChatMessage msg, String clientCid) {
        var body = Map.of(
            "messageId", msg.getId(),
            "conversationId", msg.getConversation().getId(),
            "senderId", msg.getSender().getId(),
            "content", msg.getContent(),
            "clientCid", clientCid,
            "sentAt", msg.getSentAt()
        );
        try {
            restTemplate.postForObject(realtimeBase + "/message", body, Void.class);
        } catch (Exception e) {
            System.err.println("Realtime HTTP lỗi " + e.getMessage());
        }
    }

    @Override
    public void notifyRead(Long messageId, Long userId) {
        var body = Map.of("messageId", messageId, "userId", userId);
        try {
            restTemplate.postForObject(realtimeBase + "/read", body, Void.class);
        } catch (Exception e) {
            System.err.println("Realtime HTTP lỗi " + e.getMessage());
        }
    }
}
