package com.nhom8.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom8.chat.dto.FriendOnlineDTO;
import com.nhom8.chat.dto.FriendRequestDTO;
import com.nhom8.chat.dto.FriendshipDTO;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.Friendship;
import com.nhom8.chat.entity.enums.FriendshipStatus;
import com.nhom8.chat.mapper.FriendshipMapper;
import com.nhom8.chat.repository.FriendshipRepository;
import com.nhom8.chat.repository.UserAvatarRepository;
import com.nhom8.chat.repository.UserRepository;
import com.nhom8.chat.repository.UserSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepo;
    private final UserAvatarRepository avatarRepo;

    // =====================================================
    // BUILD DTO ONLINE
    // =====================================================
    private FriendOnlineDTO buildFriendOnlineDTO(Long friendId) {

        AppUser user = userRepository.findById(friendId).orElseThrow();

        String avatar = avatarRepo.findByUserIdAndCurrentTrue(friendId)
                .map(a -> a.getMedia().getFileUrl())
                .orElse(null);

        boolean online = sessionRepo.findOnlineUsers(List.of(friendId)).contains(friendId);

        return new FriendOnlineDTO(
                friendId,
                user.getDisplayName(),
                avatar,
                online
        );
    }

    // =====================================================
    // SEND FRIEND REQUEST
    // =====================================================
    @Transactional
    public FriendshipDTO sendFriendRequest(Long currentUserId, FriendRequestDTO requestDTO) {

        Long targetUserId = requestDTO.getTargetUserId();

        if (currentUserId.equals(targetUserId))
            throw new IllegalArgumentException("Cannot send friend request to yourself");

        Long user1Id = Math.min(currentUserId, targetUserId);
        Long user2Id = Math.max(currentUserId, targetUserId);

        AppUser user1 = userRepository.findById(user1Id).orElseThrow();
        AppUser user2 = userRepository.findById(user2Id).orElseThrow();

        friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .ifPresent(ex -> {
                    throw new IllegalArgumentException("Friendship already exists");
                });

        AppUser actionUser = currentUserId.equals(user1Id) ? user1 : user2;

        Friendship f = new Friendship();
        f.setUser1(user1);
        f.setUser2(user2);
        f.setActionUser(actionUser);
        f.setStatus(FriendshipStatus.PENDING);
        f.setCreatedAt(Instant.now());
        f.setUpdatedAt(Instant.now());

        return FriendshipMapper.toDTO(friendshipRepository.save(f));
    }

    // =====================================================
    // ACCEPT REQUEST
    // =====================================================
    @Transactional
    public FriendshipDTO acceptFriendRequest(Long currentUserId, Long friendshipId) {

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (friendship.getActionUser().getId().equals(currentUserId))
            throw new IllegalArgumentException("Cannot accept your own request");

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setUpdatedAt(Instant.now());

        return FriendshipMapper.toDTO(friendshipRepository.save(friendship));
    }

    // =====================================================
    // REJECT REQUEST
    // =====================================================
    @Transactional
    public FriendshipDTO rejectFriendRequest(Long currentUserId, Long friendshipId) {

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (friendship.getActionUser().getId().equals(currentUserId))
            throw new IllegalArgumentException("Cannot reject your own request");

        friendship.setStatus(FriendshipStatus.REJECTED);
        friendship.setUpdatedAt(Instant.now());

        return FriendshipMapper.toDTO(friendshipRepository.save(friendship));
    }

    // =====================================================
    // BLOCK USER
    // =====================================================
    @Transactional
    public FriendshipDTO blockUser(Long currentUserId, Long targetUserId) {

        if (currentUserId.equals(targetUserId))
            throw new IllegalArgumentException("Cannot block yourself");

        Long user1Id = Math.min(currentUserId, targetUserId);
        Long user2Id = Math.max(currentUserId, targetUserId);

        AppUser user1 = userRepository.findById(user1Id).orElseThrow();
        AppUser user2 = userRepository.findById(user2Id).orElseThrow();

        Optional<Friendship> ex = friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id);

        Friendship f;

        if (ex.isPresent()) {
            f = ex.get();
            f.setStatus(FriendshipStatus.BLOCKED);
            f.setActionUser(currentUserId.equals(user1Id) ? user1 : user2);
            f.setUpdatedAt(Instant.now());
        } else {
            f = new Friendship();
            f.setUser1(user1);
            f.setUser2(user2);
            f.setActionUser(currentUserId.equals(user1Id) ? user1 : user2);
            f.setStatus(FriendshipStatus.BLOCKED);
            f.setCreatedAt(Instant.now());
            f.setUpdatedAt(Instant.now());
        }

        return FriendshipMapper.toDTO(friendshipRepository.save(f));
    }

    // =====================================================
    // UNBLOCK USER
    // =====================================================
    @Transactional
    public void unblockUser(Long currentUserId, Long targetUserId) {

        Friendship f = friendshipRepository.findFriendshipBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));

        if (f.getStatus() != FriendshipStatus.BLOCKED)
            throw new IllegalArgumentException("User is not blocked");

        if (!f.getActionUser().getId().equals(currentUserId))
            throw new IllegalArgumentException("Not allowed");

        friendshipRepository.delete(f);
    }

    // =====================================================
    // REMOVE FRIEND
    // =====================================================
    @Transactional
    public void removeFriend(Long currentUserId, Long friendshipId) {

        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));

        if (!f.getUser1().getId().equals(currentUserId)
                && !f.getUser2().getId().equals(currentUserId))
            throw new IllegalArgumentException("Not allowed");

        friendshipRepository.delete(f);
    }

    // =====================================================
    // FRIEND LIST (ONLINE + AVATAR)
    // =====================================================
    public List<FriendOnlineDTO> getFriends(Long currentUserId) {

        List<Friendship> list =
                friendshipRepository.findByUserIdAndStatus(currentUserId, FriendshipStatus.ACCEPTED);

        List<Long> friendIds = list.stream()
                .map(f -> f.getUser1().getId().equals(currentUserId)
                        ? f.getUser2().getId()
                        : f.getUser1().getId())
                .toList();

        List<Long> onlineUsers = sessionRepo.findOnlineUsers(friendIds);

        return friendIds.stream()
                .map(id -> {
                    AppUser u = userRepository.findById(id).orElseThrow();
                    String avatar = avatarRepo.findByUserIdAndCurrentTrue(id)
                            .map(a -> a.getMedia().getFileUrl())
                            .orElse(null);
                    boolean online = onlineUsers.contains(id);
                    return new FriendOnlineDTO(id, u.getDisplayName(), avatar, online);
                })
                .toList();
    }

    // =====================================================
    // PENDING & BLOCKED REQUESTS
    // =====================================================
    public List<FriendshipDTO> getFriendRequests(Long currentUserId) {
        return friendshipRepository.findByUserIdAndStatus(currentUserId, FriendshipStatus.PENDING)
                .stream()
                .map(FriendshipMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<FriendshipDTO> getBlockedUsers(Long currentUserId) {
        return friendshipRepository.findByUserIdAndStatus(currentUserId, FriendshipStatus.BLOCKED)
                .stream()
                .map(FriendshipMapper::toDTO)
                .collect(Collectors.toList());
    }

    // =====================================================
    // GET FRIENDSHIP RECORD BETWEEN 2 USERS
    // =====================================================
    public FriendshipDTO getFriendshipBetweenUsers(Long user1Id, Long user2Id) {
        Long a = Math.min(user1Id, user2Id);
        Long b = Math.max(user1Id, user2Id);

        return friendshipRepository.findFriendshipBetweenUsers(a, b)
                .map(FriendshipMapper::toDTO)
                .orElse(null);
    }

    // =====================================================
    // CHECK IF TWO USERS ARE FRIENDS
    // =====================================================
    public boolean areFriends(Long user1Id, Long user2Id) {
        Long a = Math.min(user1Id, user2Id);
        Long b = Math.max(user1Id, user2Id);

        return friendshipRepository.findFriendshipBetweenUsers(a, b)
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }
}