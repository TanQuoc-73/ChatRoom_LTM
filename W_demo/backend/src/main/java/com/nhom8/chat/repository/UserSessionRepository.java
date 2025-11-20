package com.nhom8.chat.repository;

import com.nhom8.chat.entity.UserSession;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionToken(String token);
    List<UserSession> findByUserIdAndOnlineTrue(Long userId);
}
