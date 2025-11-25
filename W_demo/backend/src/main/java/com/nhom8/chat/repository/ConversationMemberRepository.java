package com.nhom8.chat.repository;

import com.nhom8.chat.entity.ConversationMember;
import com.nhom8.chat.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {
    long countByIdConversationId(Long conversationId);
    List<ConversationMember> findByIdConversationId(Long conversationId);
    List<ConversationMember> findByIdUserId(Long userId);
}
