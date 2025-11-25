package com.nhom8.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom8.chat.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> { }
