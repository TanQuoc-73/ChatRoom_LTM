package com.nhom8.chat.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionToken(String token);
    List<UserSession> findByUserIdAndOnlineTrue(Long userId);
    List<UserSession> findByUserAndOnlineTrue(AppUser user);
    Optional<UserSession> findBySessionTokenAndOnlineTrue(String sessionToken);

@Query("""
SELECT us.user.id
FROM UserSession us
WHERE us.user.id IN :userIds
  AND us.lastHeartbeat >= :threshold
""")
List<Long> findOnlineUsers(
    @Param("userIds") List<Long> userIds,
    @Param("threshold") Instant threshold
);

}
