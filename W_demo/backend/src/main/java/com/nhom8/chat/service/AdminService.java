package com.nhom8.chat.service;

import com.nhom8.chat.dto.UserManagementDTO;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.enums.UserRole;
import com.nhom8.chat.exception.BusinessException;
import com.nhom8.chat.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final AppUserRepository userRepo;

    // Helper: Lấy admin hiện tại từ SecurityContext
    private AppUser getCurrentAdmin() {
        AppUser currentUser = (AppUser) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Bạn không có quyền thực hiện hành động này.");
        }
        return currentUser;
    }

    // Mapper Entity → DTO
    private UserManagementDTO toUserManagementDTO(AppUser user) {
        Long id = user.getId();
        String username = user.getUsername();
        String email = user.getEmail();
        UserRole role = user.getRole();
        Boolean active = user.getActive();
        Instant createdAt = user.getCreatedAt();

        return new UserManagementDTO(
                id != null ? id : 0L,
                username != null ? username : "N/A",
                email != null ? email : "N/A",
                role != null ? role : UserRole.USER,
                active != null ? active : false,
                createdAt != null ? createdAt : Instant.EPOCH
        );
    }

    // 1. Lấy danh sách tất cả user (phân trang)
    public Page<UserManagementDTO> getAllUsers(Pageable pageable) {
        log.info("Admin is fetching all users with pagination: {}", pageable);
        return userRepo.findAll(pageable).map(this::toUserManagementDTO);
    }

    // 2. Lấy thông tin một người dùng theo ID
    public UserManagementDTO getUserById(Long id) {
        log.info("Admin is fetching user with ID: {}", id);
        AppUser user = userRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng với ID: " + id));
        return toUserManagementDTO(user);
    }

    @Transactional // Quan trọng: đảm bảo toàn bộ thao tác là một giao dịch
    public void toggleUserStatus(Long id, boolean active) {
        String action = active ? "Mở khóa" : "Khóa";
        log.info("Admin is attempting to {} user with ID: {}", action, id);

        // 1. Tìm user. Nếu không thấy, ném exception.
        AppUser targetUser = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        // 2. Kiểm tra logic nghiệp vụ (ví dụ: admin không tự khóa mình)
        if (!active && getCurrentAdmin().getId().equals(id)) {
            throw new RuntimeException("Admin không được khóa tài khoản của chính mình.");
        }

        // 3. Chỉ CẬP NHẬT cột 'active' và thời gian cập nhật.
        targetUser.setActive(active);
        targetUser.setUpdatedAt(Instant.now());

        // 4. Lưu lại. JPA sẽ tạo ra câu lệnh SQL: UPDATE app_user SET is_active=?, updated_at=? WHERE id=?
        userRepo.save(targetUser);

        log.info("Admin successfully {}ED user [ID={}, Username={}]", action, targetUser.getId(), targetUser.getUsername());
    }

    // 4. Xóa vĩnh viễn tài khoản
    public void deleteUser(Long id) {
        log.warn("Admin is attempting to DELETE user with ID: {}", id);
        if (!userRepo.existsById(id)) {
            throw new BusinessException("Không tìm thấy người dùng để xóa.");
        }
        userRepo.deleteById(id);
        log.warn("Admin successfully DELETED user with ID: {}", id);
    }

    // Các phương thức cũ (giữ lại để phòng trường hợp)
    public void blockUser(Long targetUserId) {
        toggleUserStatus(targetUserId, false);
    }

    public void unblockUser(Long targetUserId) {
        toggleUserStatus(targetUserId, true);
    }

    public void changeUserRole(Long targetUserId, UserRole newRole) {
        AppUser admin = getCurrentAdmin();

        if (admin.getId().equals(targetUserId)) {
            throw new BusinessException("Admin không được thay đổi vai trò của chính mình.");
        }

        AppUser targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng để thay đổi vai trò."));

        if (targetUser.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN) {
            throw new BusinessException("Không thể hạ cấp vai trò của một quản trị viên khác.");
        }

        UserRole oldRole = targetUser.getRole();
        targetUser.setRole(newRole);
        targetUser.setUpdatedAt(Instant.now());
        userRepo.save(targetUser);

        log.info("Admin [ID={}] changed role of user [ID={}] from {} to {}",
                admin.getId(), targetUserId, oldRole, newRole);
    }

    public void deleteUserPermanently(Long targetUserId) {
        deleteUser(targetUserId);
    }
}