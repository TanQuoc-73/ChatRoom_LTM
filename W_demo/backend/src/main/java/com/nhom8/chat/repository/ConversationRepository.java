package com.nhom8.chat.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhom8.chat.entity.Conversation;
import com.nhom8.chat.entity.enums.ConversationType;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    Page<Conversation> findByTypeAndIsPublic(ConversationType type, Boolean isPublic, Pageable pageable);
    
    List<Conversation> findByCreatedById(Long createdById);
    
    @Query("SELECT c FROM Conversation c WHERE c.name LIKE %:keyword% OR c.description LIKE %:keyword%")
    List<Conversation> searchConversations(@Param("keyword") String keyword);
    
    @Query("SELECT c FROM Conversation c JOIN ConversationMember m ON c.id = m.conversation.id " +
           "WHERE m.user.id = :userId ORDER BY c.lastActivity DESC")
    Page<Conversation> findAllByMember(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.lastActivity > :since ORDER BY c.lastActivity DESC")
    List<Conversation> findRecentlyActiveConversations(@Param("since") java.time.Instant since);
}