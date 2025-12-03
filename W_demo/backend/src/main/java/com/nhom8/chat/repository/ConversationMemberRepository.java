package com.nhom8.chat.repository;

import com.nhom8.chat.entity.ConversationMember;
import com.nhom8.chat.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {
    
    long countByIdConversationId(Long conversationId);
    
    List<ConversationMember> findByIdConversationId(Long conversationId);
    
    List<ConversationMember> findByIdUserId(Long userId);
    
    Optional<ConversationMember> findByIdConversationIdAndIdUserId(Long conversationId, Long userId);
    
    boolean existsByIdConversationIdAndIdUserId(Long conversationId, Long userId);
    
    @Query("SELECT cm FROM ConversationMember cm WHERE cm.id.conversationId = :conversationId AND cm.role = 'CREATOR'")
    Optional<ConversationMember> findCreatorByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT cm FROM ConversationMember cm WHERE cm.id.conversationId = :conversationId AND cm.role IN ('CREATOR', 'ADMIN')")
    List<ConversationMember> findAdminsByConversationId(@Param("conversationId") Long conversationId);
    
    // THÊM: @Transactional
    @Modifying
    @Transactional
    @Query("DELETE FROM ConversationMember cm WHERE cm.id.conversationId = :conversationId")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
    
    // THÊM: Method để kiểm tra nhanh
    @Query("SELECT cm.role FROM ConversationMember cm WHERE cm.id.conversationId = :conversationId AND cm.id.userId = :userId")
    Optional<String> findRoleByConversationIdAndUserId(@Param("conversationId") Long conversationId, 
                                                       @Param("userId") Long userId);
}