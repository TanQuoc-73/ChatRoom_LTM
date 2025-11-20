package com.nhom8.chat.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.nhom8.chat.entity.TypingIndicator;

public interface TypingIndicatorRepository extends JpaRepository<TypingIndicator, Long> {
    List<TypingIndicator> findByConversationId(Long conversationId);

    @Modifying
    @Query("delete from TypingIndicator t where t.expiresAt < :now")
    int purgeExpired(Instant now);
}
