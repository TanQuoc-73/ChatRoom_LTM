package com.nhom8.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom8.chat.service.AdminService;
import com.nhom8.chat.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;
    private final AuthService authService;

    @PatchMapping("/{userId}/block")
    public ResponseEntity<?> blockUser(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long userId) {

        Long adminId = getUserId(auth);
        adminService.blockUser(adminId, userId);
        return ResponseEntity.ok().build();
    }

    private Long getUserId(String auth) {
        return authService.validateSession(auth.substring(7))
                .orElseThrow().getId();
    }

    @PatchMapping("/{userId}/unblock")
public ResponseEntity<?> unblockUser(
        @RequestHeader("Authorization") String auth,
        @PathVariable Long userId) {

    Long adminId = getUserId(auth);
    adminService.unblockUser(adminId, userId);
    return ResponseEntity.ok().build();
}

}
