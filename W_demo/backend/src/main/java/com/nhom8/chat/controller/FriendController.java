package com.nhom8.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nhom8.chat.dto.FriendOnlineDTO;
import com.nhom8.chat.dto.FriendRequestDTO;
import com.nhom8.chat.dto.FriendshipDTO;
import com.nhom8.chat.service.FriendshipService;

import lombok.RequiredArgsConstructor;

// controller quản lý quan hệ bạn bè
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipService friendshipService;

    // gửi lời mời kết bạn
    @PostMapping("/requests")
    public ResponseEntity<FriendshipDTO> sendFriendRequest(
            @RequestParam Long currentUserId,
            @RequestBody FriendRequestDTO requestDTO) {

        try {
            return ResponseEntity.ok(friendshipService.sendFriendRequest(currentUserId, requestDTO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // chấp nhận lời mời kết bạn
    @PutMapping("/requests/{friendshipId}/accept")
    public ResponseEntity<FriendshipDTO> acceptFriendRequest(
            @RequestParam Long currentUserId,
            @PathVariable Long friendshipId) {

        try {
            return ResponseEntity.ok(friendshipService.acceptFriendRequest(currentUserId, friendshipId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // từ chối lời mời kết bạn
    @PutMapping("/requests/{friendshipId}/reject")
    public ResponseEntity<FriendshipDTO> rejectFriendRequest(
            @RequestParam Long currentUserId,
            @PathVariable Long friendshipId) {

        try {
            return ResponseEntity.ok(friendshipService.rejectFriendRequest(currentUserId, friendshipId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // chặn người dùng
    @PostMapping("/block")
    public ResponseEntity<FriendshipDTO> blockUser(
            @RequestParam Long currentUserId,
            @RequestParam Long targetUserId) {

        try {
            return ResponseEntity.ok(friendshipService.blockUser(currentUserId, targetUserId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // bỏ chặn người dùng
    @DeleteMapping("/block")
    public ResponseEntity<Void> unblockUser(
            @RequestParam Long currentUserId,
            @RequestParam Long targetUserId) {

        try {
            friendshipService.unblockUser(currentUserId, targetUserId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // xóa bạn bè
    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<Void> removeFriend(
            @RequestParam Long currentUserId,
            @PathVariable Long friendshipId) {

        try {
            friendshipService.removeFriend(currentUserId, friendshipId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // lấy danh sách lời mời kết bạn
    @GetMapping("/requests")
    public ResponseEntity<List<FriendshipDTO>> getFriendRequests(@RequestParam Long currentUserId) {
        return ResponseEntity.ok(friendshipService.getFriendRequests(currentUserId));
    }

    // lấy danh sách bạn bè và trạng thái online
    @GetMapping
    public ResponseEntity<List<FriendOnlineDTO>> getFriends(@RequestParam Long currentUserId) {
        return ResponseEntity.ok(friendshipService.getFriends(currentUserId));
    }

    // lấy danh sách người bị chặn
    @GetMapping("/blocked")
    public ResponseEntity<List<FriendshipDTO>> getBlockedUsers(@RequestParam Long currentUserId) {
        return ResponseEntity.ok(friendshipService.getBlockedUsers(currentUserId));
    }

    // kiểm tra trạng thái quan hệ giữa hai user
    @GetMapping("/status")
    public ResponseEntity<FriendshipDTO> getFriendshipStatus(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id) {

        FriendshipDTO dto = friendshipService.getFriendshipBetweenUsers(user1Id, user2Id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}
