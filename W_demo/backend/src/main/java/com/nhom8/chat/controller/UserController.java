package com.nhom8.chat.controller;

import com.nhom8.chat.dto.UserProfileDTO;
import com.nhom8.chat.dto.UpdateProfileRequest;
import com.nhom8.chat.service.AuthService;
import com.nhom8.chat.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile(@RequestHeader("Authorization") String authorization) {
        try {
            Long userId = getUserIdFromToken(authorization);
            UserProfileDTO profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateMyProfile(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            Long userId = getUserIdFromToken(authorization);
            UserProfileDTO updatedProfile = userService.updateProfile(userId, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<UserProfileDTO> updateAvatar(
            @RequestHeader("Authorization") String authorization,
            @RequestParam Long mediaId) {
        try {
            Long userId = getUserIdFromToken(authorization);
            UserProfileDTO updatedProfile = userService.updateAvatar(userId, mediaId);
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/me/cover")
    public ResponseEntity<UserProfileDTO> updateCoverPhoto(
            @RequestHeader("Authorization") String authorization,
            @RequestParam Long mediaId) {
        try {
            Long userId = getUserIdFromToken(authorization);
            UserProfileDTO updatedProfile = userService.updateCoverPhoto(userId, mediaId);
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search-by-username")
    public ResponseEntity<UserProfileDTO> searchUserByUsername(@RequestParam String username) {
        Optional<UserProfileDTO> user = userService.findUserByUsername(username);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDTO>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<UserProfileDTO> users = userService.searchUsers(keyword, limit);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long userId) {
        UserProfileDTO user = userService.getUserProfile(userId);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    private Long getUserIdFromToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String sessionToken = authorization.substring(7);
            var userOpt = authService.validateSession(sessionToken);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            }
        }
        throw new IllegalArgumentException("session không hợp lệ");
    }
}