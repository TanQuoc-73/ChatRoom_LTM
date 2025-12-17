package com.nhom8.chat.realtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.nhom8.chat.entity.ChatMessage;

import chat.core.ChatService;
import chat.core.Message;
import chat.core.MessageListener;
import chat.core.protocol.Envelope;
import chat.core.protocol.MessageType;
import chat.core.spi.AuthGateway;
import chat.core.spi.MessageStore;
import chat.core.spi.RoomStore;
import chat.server.ChatServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRealtimeBridge implements MessageListener {

    private final AuthGateway authGateway;
    private final MessageStore messageStore;
    private final RoomStore roomStore;

    private ChatServer chatServer;
    private ChatService chatService;
    private final Map<String, MessageListener> httpSessions = new ConcurrentHashMap<>();
    @Autowired
private SimpMessagingTemplate messagingTemplate;


    @PostConstruct
    public void start() {
        log.info("Starting Chat Realtime Bridge...");

        try {
            // Dùng bean đã cấu hình, không new InMemory nữa
            chatService = new ChatService(authGateway, messageStore, roomStore);

            chatServer = new ChatServer(chatService);
            int chatPort = 8080;
            chatServer.start(chatPort);

            chatService.login("SYSTEM", "", this);

            log.info("✅ Chat Realtime Server started on port {}", chatPort);

        } catch (Exception e) {
            log.error("❌ Failed to start Chat Realtime Server", e);
            throw new RuntimeException("Failed to start chat realtime server", e);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Chat Realtime Bridge...");
        if (chatServer != null) {
            chatServer.stop();
            log.info("✅ Chat Realtime Server stopped");
        }
    }

    // ===== PUBLIC API =====

    public void broadcastMessage(ChatMessage chatMessage, String clientCid) {
        try {
            String roomId = "conv_" + chatMessage.getConversation().getId();
            String sender = "user_" + chatMessage.getSender().getId();
            String content = chatMessage.getContent();

            Envelope envelope = new Envelope(
                    MessageType.CHAT_MESSAGE,
                    roomId,
                    sender,
                    content
            );

            envelope.addMetadata("senderId", chatMessage.getSender().getId());
        envelope.addMetadata("senderName", chatMessage.getSender().getDisplayName());
        envelope.addMetadata("conversationId", chatMessage.getConversation().getId());
            envelope.addMetadata("messageId", chatMessage.getId());
            envelope.addMetadata("clientCid", clientCid);
            envelope.addMetadata("sentAt", chatMessage.getSentAt().toString());

            Long convId = chatMessage.getConversation().getId();
            messagingTemplate.convertAndSend(
                "/topic/conversations/" + convId,
                Map.of(
                    "id", chatMessage.getId(),
                    "conversationId", convId,
                    "senderId", chatMessage.getSender().getId(),
                    "senderName", chatMessage.getSender().getDisplayName(),
                    "content", chatMessage.getContent(),
                    "sentAt", chatMessage.getSentAt(),
                    "clientCid", clientCid
                    )
                    );


            log.debug("Broadcast message: {} from {} to room {}",
                    content, sender, roomId);

        } catch (Exception e) {
            log.error("Failed to broadcast message: {}", e.getMessage(), e);
        }
    }

    public void notifyRead(Long messageId, Long userId) {
        log.debug("Message {} read by user {}", messageId, userId);
    }

    public void userJoinConversation(Long userId, Long conversationId) {
        String username = "user_" + userId;
        String roomId = "conv_" + conversationId;

        try {
            chatService.login(username, "", this);
            chatService.joinRoom(username, roomId);
            log.debug("User {} joined conversation {}", username, roomId);
        } catch (Exception e) {
            log.error("Failed to join conversation: {}", e.getMessage(), e);
        }
    }

    public void userLeaveConversation(Long userId, Long conversationId) {
        String username = "user_" + userId;
        String roomId = "conv_" + conversationId;

        try {
            chatService.leaveRoom(username, roomId);
            log.debug("User {} left conversation {}", username, roomId);
        } catch (Exception e) {
            log.error("Failed to leave conversation: {}", e.getMessage(), e);
        }
    }

    public java.util.Set<String> getOnlineUsersInConversation(Long conversationId) {
        String roomId = "conv_" + conversationId;
        return chatService.getSessions().getUsersInRoom(roomId);
    }

    // ===== MessageListener =====

    @Override
    public void onMessage(Message message) {
        log.debug("Received message from TCP: {} in room {}",
                message.getSender(), message.getRoomId());

        String roomId = message.getRoomId();
        if (roomId.startsWith("conv_")) {
            String convId = roomId.substring(5);
            String userId = message.getSender().substring(5);

            // TODO: lưu vào DB + broadcast WebSocket
            log.info("Message from user {} in conversation {}: {}",
                    userId, convId, message.getContent());
        }
    }

    @Override
    public void onEvent(Object event) {
        if (event instanceof chat.core.protocol.Events.UserJoined e) {
            log.info("User {} joined room {}", e.username, e.roomId);
        } else if (event instanceof chat.core.protocol.Events.UserLeft e) {
            log.info("User {} left room {}", e.username, e.roomId);
        } else if (event instanceof chat.core.protocol.Events.UserTyping e) {
            log.debug("User {} {} in room {}",
                    e.username, e.typing ? "is typing" : "stopped typing", e.roomId);
        }
    }

    @Override
    public void onError(String error) { log.error("Chat error: {}", error); }

    @Override
    public void onDisconnect() { log.info("Chat disconnected"); }

    @Override
    public void onAck(String cid) { log.debug("Chat ACK: {}", cid); }

    public ChatService getChatService() { return chatService; }

    public ChatServer getChatServer() { return chatServer; }
}
