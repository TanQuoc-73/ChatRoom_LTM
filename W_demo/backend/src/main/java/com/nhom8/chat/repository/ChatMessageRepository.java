package com.nhom8.chat.repository;

import com.nhom8.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    Optional<ChatMessage> findBySenderIdAndClientCid(Long senderId, String clientCid);
    
    Page<ChatMessage> findByConversationIdOrderBySentAtDesc(Long conversationId, Pageable pageable);
  
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage m WHERE m.conversation.id = :conversationId")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.sentAt DESC")
    List<ChatMessage> findAllByConversationId(@Param("conversationId") Long conversationId);
}