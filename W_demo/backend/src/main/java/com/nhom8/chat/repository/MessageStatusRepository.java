package com.nhom8.chat.repository;

import com.nhom8.chat.entity.MessageStatus;
import com.nhom8.chat.entity.MessageStatusId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MessageStatusRepository extends JpaRepository<MessageStatus, MessageStatusId> {
    
    Optional<MessageStatus> findByMessageIdAndUserId(Long messageId, Long userId);
    
    List<MessageStatus> findByMessageId(Long messageId);
    
    List<MessageStatus> findByUserId(Long userId);
    
    @Query("SELECT ms FROM MessageStatus ms WHERE ms.user.id = :userId AND ms.readAt IS NULL")
    List<MessageStatus> findUnreadByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(ms) FROM MessageStatus ms WHERE ms.user.id = :userId AND ms.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message.conversation.id = :conversationId AND ms.user.id = :userId AND ms.readAt IS NULL")
    List<MessageStatus> findUnreadByConversationAndUser(@Param("conversationId") Long conversationId, 
                                                       @Param("userId") Long userId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM MessageStatus ms WHERE ms.message.conversation.id = :conversationId")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT COUNT(ms) FROM MessageStatus ms WHERE ms.message.conversation.id = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);
}