// src/main/java/com/nhom8/chat/repository/ChatMessageRepository.java
package com.nhom8.chat.repository;

import com.nhom8.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Optional<ChatMessage> findBySenderIdAndClientCid(Long senderId, String clientCid);
    Page<ChatMessage> findByConversationIdOrderBySentAtDesc(Long conversationId, Pageable pageable);
}
