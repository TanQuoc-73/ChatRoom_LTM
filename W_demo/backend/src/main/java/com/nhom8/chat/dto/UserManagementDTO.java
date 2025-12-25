package com.nhom8.chat.dto;

import com.nhom8.chat.entity.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Data Transfer Object (DTO) dùng để truyền thông tin người dùng
 * cho giao diện quản trị (Admin).
 * <p>
 * Lớp này giúp:
 * 1. Tránh trả về thông tin nhạy cảm của entity AppUser (ví dụ: mật khẩu).
 * 2. Tùy chỉnh dữ liệu trả về cho frontend một cách linh hoạt.
 * 3. Tách biệt cấu trúc dữ liệu của API khỏi cấu trúc của cơ sở dữ liệu.
 */
public class UserManagementDTO {

    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private boolean active;

    /**
     * Sử dụng Instant để khớp với AppUser entity.
     * @JsonFormat giúp định dạng thời gian khi trả về JSON cho frontend.
     * timezone là quan trọng để hiển thị đúng giờ địa phương.
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant createdAt;

    /**
     * Constructor mặc định (không tham số).
     * Cần thiết cho các thư viện như Jackson khi deserialize JSON thành object.
     */
    public UserManagementDTO() {
    }

    /**
     * Constructor đầy đủ tham số.
     * Tiện lợi để tạo đối tượng DTO từ entity AppUser.
     */
    public UserManagementDTO(Long id, String username, String email, UserRole role, boolean active, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    // --- GETTERS AND SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // --- (Tùy chọn) toString để tiện debug ---
    @Override
    public String toString() {
        return "UserManagementDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}