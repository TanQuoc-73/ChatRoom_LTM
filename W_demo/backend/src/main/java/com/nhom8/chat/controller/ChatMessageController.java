package com.nhom8.chat.controller;

import com.nhom8.chat.entity.ChatMessage;
import com.nhom8.chat.dto.TypingNotification;
import com.nhom8.chat.dto.ReadNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat.typing")
public void typing(@Payload TypingNotification notification) {
    messagingTemplate.convertAndSend(
        "/topic/conversations/" + notification.getConversationId() + "/typing",
        notification
    );
}

@MessageMapping("/chat.read")
public void markAsRead(@Payload ReadNotification notification) {
    messagingTemplate.convertAndSend(
        "/topic/conversations/" + notification.getConversationId() + "/read",
        notification
    );
}

}