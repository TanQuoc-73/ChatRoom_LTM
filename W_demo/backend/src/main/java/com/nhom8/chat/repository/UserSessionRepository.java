package com.nhom8.chat.repository;

import com.nhom8.chat.entity.UserSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionToken(String token);
    List<UserSession> findByUserIdAndOnlineTrue(Long userId);
    Optional<UserSession> findBySessionTokenAndOnlineTrue(String sessionToken);

    @Query("""
    SELECT us.user.id
    FROM UserSession us
    WHERE us.user.id IN :userIds AND us.online = true
""")
List<Long> findOnlineUsers(@Param("userIds") List<Long> userIds);

}
