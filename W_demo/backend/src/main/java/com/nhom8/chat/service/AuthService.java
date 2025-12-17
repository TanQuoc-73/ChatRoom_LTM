package com.nhom8.chat.service;

import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.UserSession;
import com.nhom8.chat.entity.enums.DeviceType;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;

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

    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
