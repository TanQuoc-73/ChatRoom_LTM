package com.nhom8.chat.repository;

import com.nhom8.chat.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    
    List<MessageAttachment> findByMessageIdOrderBySortOrderAsc(Long messageId);

    @Modifying
    @Transactional
    @Query("DELETE FROM MessageAttachment ma WHERE ma.message.conversation.id = :conversationId")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT COUNT(ma) FROM MessageAttachment ma WHERE ma.message.conversation.id = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);
}