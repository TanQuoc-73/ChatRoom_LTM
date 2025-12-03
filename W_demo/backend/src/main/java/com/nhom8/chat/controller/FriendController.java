package com.nhom8.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom8.chat.dto.FriendRequestDTO;
import com.nhom8.chat.dto.FriendshipDTO;
import com.nhom8.chat.service.FriendshipService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipService friendshipService;

    @PostMapping("/requests")
    public ResponseEntity<FriendshipDTO> sendFriendRequest(
            @RequestParam Long currentUserId,
            @RequestBody FriendRequestDTO requestDTO) {
        try {
            FriendshipDTO result = friendshipService.sendFriendRequest(currentUserId, requestDTO);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/requests/{friendshipId}/accept")
    public ResponseEntity<FriendshipDTO> acceptFriendRequest(
            @RequestParam Long currentUserId,
            @PathVariable Long friendshipId) {
        try {
            FriendshipDTO result = friendshipService.acceptFriendRequest(currentUserId, friendshipId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/requests/{friendshipId}/reject")
    public ResponseEntity<FriendshipDTO> rejectFriendRequest(
            @RequestParam Long currentUserId,
            @PathVariable Long friendshipId) {
        try {
            FriendshipDTO result = friendshipService.rejectFriendRequest(currentUserId, friendshipId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/block")
    public ResponseEntity<FriendshipDTO> blockUser(
            @RequestParam Long currentUserId,
            @RequestParam Long targetUserId) {
        try {
            FriendshipDTO result = friendshipService.blockUser(currentUserId, targetUserId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

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

    @GetMapping("/requests")
    public ResponseEntity<List<FriendshipDTO>> getFriendRequests(@RequestParam Long currentUserId) {
        List<FriendshipDTO> requests = friendshipService.getFriendRequests(currentUserId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping
    public ResponseEntity<List<FriendshipDTO>> getFriends(@RequestParam Long currentUserId) {
        List<FriendshipDTO> friends = friendshipService.getFriends(currentUserId);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<FriendshipDTO>> getBlockedUsers(@RequestParam Long currentUserId) {
        List<FriendshipDTO> blocked = friendshipService.getBlockedUsers(currentUserId);
        return ResponseEntity.ok(blocked);
    }

    @GetMapping("/status")
    public ResponseEntity<FriendshipDTO> getFriendshipStatus(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id) {
        FriendshipDTO friendship = friendshipService.getFriendshipBetweenUsers(user1Id, user2Id);
        return friendship != null ? ResponseEntity.ok(friendship) : ResponseEntity.notFound().build();
    }
}