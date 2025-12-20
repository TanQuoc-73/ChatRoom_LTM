package com.nhom8.chat.service;

import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.PasswordResetToken;
import com.nhom8.chat.entity.UserSession;
import com.nhom8.chat.entity.enums.DeviceType;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.UserSessionRepository;
import com.nhom8.chat.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AppUser register(String username, String email, String plainPassword, 
                           String displayName, String firstName, String lastName) {
        
        if (appUserRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Tồn tại mất rồi " + username);
        }
        if (appUserRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại " + email);
        }

        String passwordHash = passwordEncoder.encode(plainPassword);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(true);
        user.setVerified(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        return appUserRepository.save(user);
    }

   public UserSession login(String username, String plainPassword, DeviceType deviceType, 
                         String clientInfo, String ipAddress) {

    AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Sai tên hoặc pass rồi"));

    if (!passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
        throw new IllegalArgumentException("Sai tên hoặc pass rồi");
    }

    if (!user.getActive()) {
        throw new IllegalArgumentException("Tài khoản bị vô hiệu hóa -1");
    }

    userSessionRepository.findByUserIdAndOnlineTrue(user.getId())
            .forEach(s -> {
                s.setOnline(false);
                userSessionRepository.save(s);
            });

    user.setLastActive(Instant.now());
    appUserRepository.save(user);

    UserSession session = new UserSession();
    session.setUser(user);
    session.setSessionToken(generateSessionToken());
    session.setDeviceType(deviceType);
    session.setClientInfo(clientInfo);
    session.setIpAddress(ipAddress);
    session.setOnline(true);
    session.setConnectedAt(Instant.now());
    session.setLastHeartbeat(Instant.now());

    return userSessionRepository.save(session);
}


    public void logout(String sessionToken) {
        UserSession session = userSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new IllegalArgumentException("session token ko hợp lệ"));
        
        session.setOnline(false);
        userSessionRepository.save(session);
    }

public Optional<AppUser> validateSession(String sessionToken) {
    Optional<UserSession> sessionOpt = userSessionRepository.findBySessionToken(sessionToken);
    
    if (sessionOpt.isPresent()) {
        UserSession session = sessionOpt.get();
        if (session.isOnline() && 
            session.getLastHeartbeat().isAfter(Instant.now().minusSeconds(3600))) {
 
            session.setLastHeartbeat(Instant.now());
            userSessionRepository.save(session);
            
            return Optional.of(session.getUser());
        }
    }
    
    return Optional.empty();
}

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không thấy người dùng bro ơi"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Sai pass rồi huhu");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        appUserRepository.save(user);

        return true;
    }
    public void requestPasswordReset(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại trong hệ thống"));

        if (!user.getActive()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
        }

        // Invalidate all previous unused OTP tokens for this user
        List<PasswordResetToken> oldTokens = passwordResetTokenRepository.findByUserAndUsedFalse(user);
        oldTokens.forEach(token -> {
            token.setUsed(true);
            token.setUsedAt(Instant.now());
        });
        passwordResetTokenRepository.saveAll(oldTokens);

        // Generate new 6-digit OTP
        String otp = generateOtp();

        // Create new reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setOtp(otp);
        passwordResetTokenRepository.save(resetToken);

        // Send OTP via email
        emailService.sendOtpEmail(user.getEmail(), otp, user.getUsername());
    }

    public void resetPassword(String email, String otp, String newPassword) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại trong hệ thống"));

        // Find valid OTP token
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByUserAndOtpAndUsedFalseAndExpiresAtAfter(user, otp, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn"));

        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        appUserRepository.save(user);

        // Send confirmation email
        emailService.sendPasswordResetConfirmation(user.getEmail(), user.getUsername());

        // Logout all active sessions for security
        List<UserSession> activeSessions = userSessionRepository.findByUserAndOnlineTrue(user);
        activeSessions.forEach(session -> session.setOnline(false));
        userSessionRepository.saveAll(activeSessions);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(otp);
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
