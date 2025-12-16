package com.nhom8.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom8.chat.dto.AuthResponse;
import com.nhom8.chat.dto.LoginRequest;
import com.nhom8.chat.dto.RegisterRequest;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.UserSession;
import com.nhom8.chat.entity.enums.DeviceType;
import com.nhom8.chat.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// controller xử lý các chức năng xác thực người dùng
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // api đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            AppUser user = authService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName(),
                request.getFirstName(),
                request.getLastName()
            );

            // tự động đăng nhập và tạo session sau khi đăng ký
            UserSession session = authService.login(
                    user.getUsername(),
                    request.getPassword(),
                    DeviceType.WEB,
                    httpRequest.getHeader("User-Agent"),
                    httpRequest.getRemoteAddr()
            );

            return ResponseEntity.ok(AuthResponse.success(
                "Đăng ký thành kông", 
                user.getId(), 
                user.getUsername(),
                session.getSessionToken()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AuthResponse.error("Đăng ký 7 bại " + e.getMessage()));
        }
    }

    // api đăng nhập
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, 
                                            HttpServletRequest httpRequest) {
        try {
            UserSession session = authService.login(
                request.getUsername(),
                request.getPassword(),
                request.getDeviceType(),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
            );

            return ResponseEntity.ok(AuthResponse.success(
                "Đăng nhập thành kông",
                session.getUser().getId(),
                session.getUser().getUsername(),
                session.getSessionToken()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AuthResponse.error("Đăng nhập 7 bại " + e.getMessage()));
        }
    }

    // api đăng xuất
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String authorization) {
        try {
            String sessionToken = extractSessionToken(authorization);
            authService.logout(sessionToken);
            
            return ResponseEntity.ok(AuthResponse.success("Đăng xuất thành kông, đừng đi mà", null, null, null));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AuthResponse.error("Lỗi đăng xuất" + e.getMessage()));
        }
    }

    // api kiểm tra tính hợp lệ của session
    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateSession(@RequestHeader("Authorization") String authorization) {
        try {
            String sessionToken = extractSessionToken(authorization);
            var userOpt = authService.validateSession(sessionToken);
            
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                return ResponseEntity.ok(AuthResponse.success(
                    "Phiên hợp lệ",
                    user.getId(),
                    user.getUsername(),
                    sessionToken
                ));
            } else {
                return ResponseEntity.status(401).body(AuthResponse.error("Phiên ko hợp lệ hoặc hết hạn"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(401).body(AuthResponse.error("Xác thực phiên ko thành kông"));
        }
    }

    // tách session token từ header authorization
    private String extractSessionToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        throw new IllegalArgumentException("Ủy quyền không hợp lệ");
    }
}
