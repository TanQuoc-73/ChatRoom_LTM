package com.nhom8.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom8.chat.entity.BlockedUser;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> { }
