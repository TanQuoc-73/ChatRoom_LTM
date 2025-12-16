package com.nhom8.chat.repository;

import java.util.List;
import java.util.Optional;

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
    
    @Query("SELECT c FROM Conversation c WHERE c.type = 'DIRECT' AND " +
           "EXISTS (SELECT 1 FROM ConversationMember cm1 WHERE cm1.conversation = c AND cm1.user.id = :user1Id) AND " +
           "EXISTS (SELECT 1 FROM ConversationMember cm2 WHERE cm2.conversation = c AND cm2.user.id = :user2Id)")
    Optional<Conversation> findDirectConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT c FROM Conversation c JOIN ConversationMember m ON c.id = m.conversation.id " +
           "WHERE c.type = :type AND m.user.id = :userId")
    List<Conversation> findByTypeAndMember(@Param("type") ConversationType type, @Param("userId") Long userId);
}