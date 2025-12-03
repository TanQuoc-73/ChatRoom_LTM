package com.nhom8.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom8.chat.dto.FriendRequestDTO;
import com.nhom8.chat.dto.FriendshipDTO;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.Friendship;
import com.nhom8.chat.entity.enums.FriendshipStatus;
import com.nhom8.chat.mapper.FriendshipMapper;
import com.nhom8.chat.repository.FriendshipRepository;
import com.nhom8.chat.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

@Transactional
public FriendshipDTO sendFriendRequest(Long currentUserId, FriendRequestDTO requestDTO) {
    if (requestDTO == null || requestDTO.getTargetUserId() == null) {
        throw new IllegalArgumentException("Target user ID cannot be null");
    }
    
    Long targetUserId = requestDTO.getTargetUserId();
    
    // Kiểm tra không thể kết bạn với chính mình
    if (currentUserId.equals(targetUserId)) {
        throw new IllegalArgumentException("Cannot send friend request to yourself");
    }

    // ĐẢM BẢO user1Id LUÔN NHỎ HƠN user2Id
    Long user1Id = Math.min(currentUserId, targetUserId);
    Long user2Id = Math.max(currentUserId, targetUserId);
    
    // SỬA: Tìm user theo user1Id và user2Id đã sắp xếp
    AppUser user1 = userRepository.findById(user1Id)
            .orElseThrow(() -> new IllegalArgumentException("User1 not found"));
    AppUser user2 = userRepository.findById(user2Id)
            .orElseThrow(() -> new IllegalArgumentException("User2 not found"));

    // SỬA: Kiểm tra friendship với user1Id và user2Id đã sắp xếp
    friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Friendship already exists with status: " + existing.getStatus());
            });

    // Xác định ai là người gửi request
    AppUser actionUser = currentUserId.equals(user1Id) ? user1 : user2;

    // SỬA: Tạo friendship mới với user1 và user2 đã sắp xếp
    Friendship friendship = new Friendship();
    friendship.setUser1(user1);  // user1 nhỏ hơn
    friendship.setUser2(user2);  // user2 lớn hơn
    friendship.setStatus(FriendshipStatus.PENDING);
    friendship.setActionUser(actionUser);  // Người thực sự gửi request
    friendship.setCreatedAt(Instant.now());
    friendship.setUpdatedAt(Instant.now());

    Friendship saved = friendshipRepository.save(friendship);
    log.info("Friend request sent from user {} to user {}", currentUserId, targetUserId);
    
    return FriendshipMapper.toDTO(saved);
}

@Transactional
public FriendshipDTO acceptFriendRequest(Long currentUserId, Long friendshipId) {
    Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

    // KIỂM TRA: currentUser KHÔNG PHẢI là người gửi request (actionUser)
    if (friendship.getActionUser().getId().equals(currentUserId)) {
        throw new IllegalArgumentException("You cannot accept your own friend request");
    }

    // Kiểm tra status phải là PENDING
    if (friendship.getStatus() != FriendshipStatus.PENDING) {
        throw new IllegalArgumentException("Friend request is not pending");
    }

    // CHỈ UPDATE status, KHÔNG thay đổi user1/user2/actionUser
    friendship.setStatus(FriendshipStatus.ACCEPTED);
    friendship.setUpdatedAt(Instant.now());

    Friendship updated = friendshipRepository.save(friendship);
    log.info("Friend request accepted: {}", friendshipId);
    
    return FriendshipMapper.toDTO(updated);
}

@Transactional
public FriendshipDTO rejectFriendRequest(Long currentUserId, Long friendshipId) {
    Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

    // KIỂM TRA: currentUser KHÔNG PHẢI là người gửi request (actionUser)
    if (friendship.getActionUser().getId().equals(currentUserId)) {
        throw new IllegalArgumentException("You cannot reject your own friend request");
    }

    friendship.setStatus(FriendshipStatus.REJECTED);
    // CHỈ UPDATE status, KHÔNG thay đổi user1/user2/actionUser
    friendship.setUpdatedAt(Instant.now());

    Friendship updated = friendshipRepository.save(friendship);
    log.info("Friend request rejected: {}", friendshipId);
    
    return FriendshipMapper.toDTO(updated);
}
   @Transactional
public FriendshipDTO blockUser(Long currentUserId, Long targetUserId) {
    
    // Kiểm tra không thể block chính mình
    if (currentUserId.equals(targetUserId)) {
        throw new IllegalArgumentException("Cannot block yourself");
    }

    // ĐẢM BẢO user1Id < user2Id
    Long user1Id = Math.min(currentUserId, targetUserId);
    Long user2Id = Math.max(currentUserId, targetUserId);
    
    AppUser user1 = userRepository.findById(user1Id)
            .orElseThrow(() -> new IllegalArgumentException("User1 not found"));
    AppUser user2 = userRepository.findById(user2Id)
            .orElseThrow(() -> new IllegalArgumentException("User2 not found"));
    
    // Tìm friendship với đúng thứ tự đã sắp xếp
    Optional<Friendship> existingFriendshipOpt = friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id);
    
    Friendship friendship;
    
    if (existingFriendshipOpt.isPresent()) {
        // Nếu đã tồn tại, dùng friendship hiện tại
        friendship = existingFriendshipOpt.get();
        
        // CHỈ UPDATE status và actionUser, KHÔNG thay đổi user1/user2
        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendship.setActionUser(currentUserId.equals(user1Id) ? user1 : user2);
        friendship.setUpdatedAt(Instant.now());
        
    } else {
        // Tạo mới với đúng thứ tự user1 < user2
        friendship = new Friendship();
        friendship.setUser1(user1);  // user1 nhỏ hơn
        friendship.setUser2(user2);  // user2 lớn hơn
        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendship.setActionUser(currentUserId.equals(user1Id) ? user1 : user2);
        friendship.setCreatedAt(Instant.now());
        friendship.setUpdatedAt(Instant.now());
    }

    Friendship saved = friendshipRepository.save(friendship);
    log.info("User {} blocked user {}", currentUserId, targetUserId);
    
    return FriendshipMapper.toDTO(saved);
}

    @Transactional
    public void unblockUser(Long currentUserId, Long targetUserId) {
        Friendship friendship = friendshipRepository.findFriendshipBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("No friendship found"));

        if (friendship.getStatus() != FriendshipStatus.BLOCKED) {
            throw new IllegalArgumentException("User is not blocked");
        }

        if (!friendship.getActionUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Only the user who blocked can unblock");
        }

        friendshipRepository.delete(friendship);
        log.info("User {} unblocked user {}", currentUserId, targetUserId);
    }

    @Transactional
    public void removeFriend(Long currentUserId, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        // Kiểm tra current user có liên quan đến friendship này không
        if (!friendship.getUser1().getId().equals(currentUserId) && 
            !friendship.getUser2().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not part of this friendship");
        }

        friendshipRepository.delete(friendship);
        log.info("Friendship removed: {}", friendshipId);
    }

    public List<FriendshipDTO> getFriendRequests(Long currentUserId) {
        List<Friendship> pendingRequests = friendshipRepository.findByUserIdAndStatus(
                currentUserId, FriendshipStatus.PENDING);
        
        return pendingRequests.stream()
                .map(FriendshipMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<FriendshipDTO> getFriends(Long currentUserId) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(
                currentUserId, FriendshipStatus.ACCEPTED);
        
        return friendships.stream()
                .map(FriendshipMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<FriendshipDTO> getBlockedUsers(Long currentUserId) {
        List<Friendship> blocked = friendshipRepository.findByUserIdAndStatus(
                currentUserId, FriendshipStatus.BLOCKED);
        
        return blocked.stream()
                .map(FriendshipMapper::toDTO)
                .collect(Collectors.toList());
    }

    public FriendshipDTO getFriendshipBetweenUsers(Long user1Id, Long user2Id) {
        return friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .map(FriendshipMapper::toDTO)
                .orElse(null);
    }

public boolean areFriends(Long user1Id, Long user2Id) {
    return friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
            .map(friendship -> friendship.getStatus().equals(FriendshipStatus.ACCEPTED))
            .orElse(false);
}
}