package com.nhom8.chat.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom8.chat.dto.AddMemberRequest;
import com.nhom8.chat.dto.ConversationCreateRequest;
import com.nhom8.chat.dto.ConversationDto;
import com.nhom8.chat.dto.ConversationMemberDto;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.Conversation;
import com.nhom8.chat.entity.ConversationMember;
import com.nhom8.chat.entity.ConversationMemberId;
import com.nhom8.chat.entity.Media;
import com.nhom8.chat.entity.UserAvatar;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.ConversationMemberRepository;
import com.nhom8.chat.repository.UserAvatarRepository;
import com.nhom8.chat.service.ConversationService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;


import java.util.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@Slf4j


public class ConversationController {

    private final ConversationService convService;
    private final ConversationMemberRepository memberRepo; 
    private final AppUserRepository userRepo;
    private final UserAvatarRepository userAvatarRepo;
    
    

    @PostMapping
    public ResponseEntity<ConversationDto> create(
            @Validated @RequestBody ConversationCreateRequest req,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        Conversation c = convService.createConversation(req, userId);

        ConversationDto dto = ConversationDto.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType().name())
                .description(c.getDescription())
                .createdBy(c.getCreatedBy().getId())
                .isPublic(Boolean.TRUE.equals(c.getIsPublic()))
                .maxMembers(c.getMaxMembers())
                .lastMessageId(c.getLastMessageId())
                .build();

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Validated @RequestBody ConversationCreateRequest req,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        convService.updateConversation(id, req, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        convService.deleteConversation(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(
            @PathVariable Long id,
            @Validated @RequestBody AddMemberRequest req,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        convService.addMember(id, req, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<?> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        convService.removeMember(id, memberId, userId);
        return ResponseEntity.noContent().build();
    }

     @GetMapping("/{id}")
    public ResponseEntity<ConversationDto> getOne(@PathVariable Long id, Principal principal) {
        // nếu muốn kiểm tra quyền thành viên thì thêm ở Service
        Conversation c = convService.getConversation(id);

        ConversationDto dto = ConversationDto.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType().name())
                .description(c.getDescription())
                .createdBy(c.getCreatedBy().getId())
                .isPublic(Boolean.TRUE.equals(c.getIsPublic()))
                .maxMembers(c.getMaxMembers())
                .lastMessageId(c.getLastMessageId())
                .build();

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ConversationDto>> myConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());

        Pageable pageable = PageRequest.of(
                page,
                size
        );

        Page<Conversation> pg = convService.listConversationsForMember(userId, pageable);

        Page<ConversationDto> dtoPage = pg.map(c -> ConversationDto.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType().name())
                .description(c.getDescription())
                .createdBy(c.getCreatedBy().getId())
                .isPublic(Boolean.TRUE.equals(c.getIsPublic()))
                .maxMembers(c.getMaxMembers())
                .lastMessageId(c.getLastMessageId())
                .build()
        );

        return ResponseEntity.ok(dtoPage);
    }
    // Thêm vào ConversationController.java
@PostMapping("/direct/{friendId}")
public ResponseEntity<ConversationDto> createOrGetDirectConversation(
        @PathVariable Long friendId,
        Principal principal) {
    Long userId = Long.parseLong(principal.getName());
    
    Conversation directConv = convService.getOrCreateDirectConversation(userId, friendId);
    
    ConversationDto dto = ConversationDto.builder()
            .id(directConv.getId())
            .name(directConv.getName())
            .type(directConv.getType().name())
            .description(directConv.getDescription())
            .createdBy(directConv.getCreatedBy().getId())
            .isPublic(Boolean.TRUE.equals(directConv.getIsPublic()))
            .maxMembers(directConv.getMaxMembers())
            .lastMessageId(directConv.getLastMessageId())
            .build();
    
    return ResponseEntity.ok(dto);
}

@PostMapping("/group")
public ResponseEntity<ConversationDto> createGroupConversation(
        @Validated @RequestBody ConversationCreateRequest req,
        Principal principal) {
    Long userId = Long.parseLong(principal.getName());
    
    Conversation groupConv = convService.createGroupConversation(req, userId);
    
    ConversationDto dto = ConversationDto.builder()
            .id(groupConv.getId())
            .name(groupConv.getName())
            .type(groupConv.getType().name())
            .description(groupConv.getDescription())
            .createdBy(groupConv.getCreatedBy().getId())
            .isPublic(Boolean.TRUE.equals(groupConv.getIsPublic()))
            .maxMembers(groupConv.getMaxMembers())
            .lastMessageId(groupConv.getLastMessageId())
            .build();
    
    return ResponseEntity.ok(dto);
}

  @GetMapping("/debug/status/{id}")
    public ResponseEntity<Map<String, Object>> debugConversationStatus(
            @PathVariable Long id,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = Long.parseLong(principal.getName());
            
            // 1. Kiểm tra conversation tồn tại không
            Optional<Conversation> convOpt = convService.getConversationOpt(id);
            response.put("existsInRepo", convOpt.isPresent());
            
            if (convOpt.isPresent()) {
                Conversation conv = convOpt.get();
                response.put("conversation", Map.of(
                    "id", conv.getId(),
                    "name", conv.getName(),
                    "type", conv.getType(),
                    "createdBy", conv.getCreatedBy().getId(),
                    "createdAt", conv.getCreatedAt()
                ));
                
                // 2. Đếm members
                long memberCount = memberRepo.countByIdConversationId(id);
                response.put("memberCount", memberCount);
                
                // 3. Kiểm tra quyền của user hiện tại
                ConversationMemberId memberId = new ConversationMemberId(id, userId);
                boolean isMember = memberRepo.existsById(memberId);
                response.put("currentUserIsMember", isMember);
                
                if (isMember) {
                    Optional<ConversationMember> memberOpt = memberRepo.findById(memberId);
                    memberOpt.ifPresent(member -> response.put("currentUserRole", member.getRole()));
                }
                
                // 4. Kiểm tra user có phải creator không
                boolean isCreator = conv.getCreatedBy().getId().equals(userId);
                response.put("currentUserIsCreator", isCreator);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Debug error: ", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // THÊM: Force delete endpoint (cho admin/debug)
    @PostMapping("/debug/force-delete/{id}")
    public ResponseEntity<Map<String, Object>> forceDeleteConversation(
            @PathVariable Long id,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Force delete requested for conversation {} by user {}", id, userId);
            
            // Kiểm tra conversation tồn tại không
            Optional<Conversation> convOpt = convService.getConversationOpt(id);
            if (!convOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Conversation not found");
                response.put("conversationId", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Gọi service để xóa
            convService.deleteConversation(id, userId);
            
            response.put("success", true);
            response.put("message", "Conversation deleted successfully");
            response.put("conversationId", id);
            
            // Kiểm tra lại sau khi xóa
            boolean stillExists = convService.getConversationOpt(id).isPresent();
            response.put("stillExistsAfterDelete", stillExists);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Force delete failed: ", e);
            
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("conversationId", id);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
@GetMapping("/{id}/members")
    public ResponseEntity<List<ConversationMemberDto>> getMembers(
            @PathVariable Long id,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        
        // Kiểm tra user có phải là thành viên không
        ConversationMemberId memberId = new ConversationMemberId(id, userId);
        if (!memberRepo.existsById(memberId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<ConversationMember> members = memberRepo.findByIdConversationId(id);
        
        List<ConversationMemberDto> dtos = members.stream()
            .map(m -> {
                AppUser user = m.getUser();
                String avatarUrl = getAvatarUrlForUser(user);
                
                return new ConversationMemberDto(
                    user.getId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    avatarUrl,
                    m.getRole(),
                    m.getNickname(),
                    m.isMuted()
                );
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    // THÊM: Helper method để lấy avatar URL
    private String getAvatarUrlForUser(AppUser user) {
        try {
            // Lấy avatar hiện tại của user
            Optional<UserAvatar> currentAvatarOpt = userAvatarRepo.findByUserIdAndCurrentTrue(user.getId());
            
            if (currentAvatarOpt.isPresent()) {
                UserAvatar userAvatar = currentAvatarOpt.get();
                Media media = userAvatar.getMedia();
                if (media != null && media.getFileUrl() != null) {
                    return media.getFileUrl();
                }
            }
        } catch (Exception e) {
            // Log lỗi nhưng không làm crash
            System.err.println("Error getting avatar for user " + user.getId() + ": " + e.getMessage());
        }
        
        return null; // Hoặc trả về URL ảnh mặc định
    }

    // THÊM: Endpoint lấy số lượng thành viên
    @GetMapping("/{id}/members/count")
    public ResponseEntity<Map<String, Object>> getMemberCount(
            @PathVariable Long id,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        
        // Kiểm tra user có phải là thành viên không
        ConversationMemberId memberId = new ConversationMemberId(id, userId);
        if (!memberRepo.existsById(memberId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        long count = memberRepo.countByIdConversationId(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", id);
        response.put("memberCount", count);
        
        return ResponseEntity.ok(response);
    }

    // THÊM: Endpoint kiểm tra user có phải là thành viên
    @GetMapping("/{id}/members/me")
    public ResponseEntity<Map<String, Object>> checkMyMembership(
            @PathVariable Long id,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        
        ConversationMemberId memberId = new ConversationMemberId(id, userId);
        Optional<ConversationMember> memberOpt = memberRepo.findById(memberId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", id);
        response.put("userId", userId);
        response.put("isMember", memberOpt.isPresent());
        
        if (memberOpt.isPresent()) {
            ConversationMember member = memberOpt.get();
            response.put("role", member.getRole());
            response.put("joinedAt", member.getJoinedAt());
            response.put("nickname", member.getNickname());
        }
        
        return ResponseEntity.ok(response);
    }
    
}
