package com.nhom8.chat.controller;

import com.nhom8.chat.dto.UserManagementDTO;
import com.nhom8.chat.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // 1. Lấy danh sách user
    @GetMapping
    public Page<UserManagementDTO> getAllUsers(Pageable pageable) {
        return adminService.getAllUsers(pageable);
    }

    // --- THÊM LOG VÀO PHƯƠNG THỨC NÀY ---
    @GetMapping("/{id}")
    public ResponseEntity<UserManagementDTO> getUserById(@PathVariable Long id) {
        System.out.println(">>> BACKEND: getUserById() method was called with ID: " + id); // <-- VÀ DÒNG NÀY
        try {
            UserManagementDTO user = adminService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 2. Khóa/Mở khóa user
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long id, @RequestParam boolean active) {
        try {
            adminService.toggleUserStatus(id, active);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 3. Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}