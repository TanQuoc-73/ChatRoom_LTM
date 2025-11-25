package com.nhom8.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom8.chat.dto.AuthResponse;
import com.nhom8.chat.dto.ForgotPasswordRequest;
import com.nhom8.chat.dto.LoginRequest;
import com.nhom8.chat.dto.RegisterRequest;
import com.nhom8.chat.dto.ResetPasswordRequest;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.UserSession;
import com.nhom8.chat.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AppUser user = authService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName(),
                request.getFirstName(),
                request.getLastName()
            );

            return ResponseEntity.ok(AuthResponse.success(
                "Đăng ký thành kông", 
                user.getId(), 
                user.getUsername(),
                null
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AuthResponse.error("Đăng ký 7 bại " + e.getMessage()));
        }
    }

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

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.requestPasswordReset(request.getEmail());
            
            return ResponseEntity.ok(AuthResponse.success(
                "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.",
                null,
                null,
                null
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AuthResponse.error("Không thể gửi OTP. Vui lòng thử lại sau."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
            );
            
            return ResponseEntity.ok(AuthResponse.success(
                "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.",
                null,
                null,
                null
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AuthResponse.error("Không thể đặt lại mật khẩu. Vui lòng thử lại."));
        }
    }

    private String extractSessionToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        throw new IllegalArgumentException("Ủy quyền không hợp lệ");
    }
}