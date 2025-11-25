package com.nhom8.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom8.chat.entity.MessageAttachment;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    List<MessageAttachment> findByMessageIdOrderBySortOrderAsc(Long messageId);
}
