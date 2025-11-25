package com.nhom8.chat.repository;

import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByUserAndOtpAndUsedFalseAndExpiresAtAfter(
        AppUser user, 
        String otp, 
        Instant now
    );
    
    List<PasswordResetToken> findByUserAndUsedFalse(AppUser user);
    
    void deleteByExpiresAtBefore(Instant now);
}
