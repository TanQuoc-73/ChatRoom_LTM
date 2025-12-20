package com.nhom8.chat.service;

import org.springframework.stereotype.Service;

import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.enums.UserRole;
import com.nhom8.chat.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AppUserRepository userRepo;

    public void blockUser(Long adminId, Long targetUserId) {
        AppUser admin = userRepo.findById(adminId).orElseThrow();

        if (admin.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Chỉ admin được khóa user");
        }

        AppUser user = userRepo.findById(targetUserId).orElseThrow();
        user.setActive(false);
        userRepo.save(user);
    }

    public void unblockUser(Long adminId, Long targetUserId) {
    AppUser admin = userRepo.findById(adminId).orElseThrow();

    if (admin.getRole() != UserRole.ADMIN) {
        throw new SecurityException("Chỉ admin được mở khóa");
    }

    AppUser user = userRepo.findById(targetUserId).orElseThrow();
    user.setActive(true);
    userRepo.save(user);


}
}