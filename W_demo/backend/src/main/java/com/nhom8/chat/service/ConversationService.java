package com.nhom8.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom8.chat.dto.AddMemberRequest;
import com.nhom8.chat.dto.ConversationCreateRequest;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.Conversation;
import com.nhom8.chat.entity.ConversationMember;
import com.nhom8.chat.entity.ConversationMemberId;
import com.nhom8.chat.entity.enums.ConversationType;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.ChatMessageRepository;
import com.nhom8.chat.repository.ConversationMemberRepository;
import com.nhom8.chat.repository.ConversationRepository;
import com.nhom8.chat.repository.MessageAttachmentRepository;
import com.nhom8.chat.repository.MessageStatusRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository convRepo;
    private final ConversationMemberRepository memberRepo;
    private final AppUserRepository userRepo;
    private final FriendshipService friendshipService;
    private final ChatMessageRepository messageRepo;
    private final MessageStatusRepository statusRepo;
    private final MessageAttachmentRepository attachmentRepo;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Conversation createConversation(ConversationCreateRequest req, Long creatorId) {
        AppUser creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        ConversationType typeEnum = parseTypeOrDefault(req.getType(), ConversationType.GROUP);

        // Kiểm tra điều kiện tạo nhóm
        if (typeEnum == ConversationType.GROUP) {
            validateGroupCreation(req, creatorId);
        }

        Conversation conv = Conversation.builder()
                .type(typeEnum)
                .name(req.getName())
                .description(req.getDescription())
                .createdBy(creator)
                .avatarMedia(null)
                .isPublic(Boolean.TRUE.equals(req.getIsPublic()))
                .maxMembers(req.getMaxMembers())
                .lastMessageId(null)
                .lastActivity(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Conversation saved = convRepo.save(conv);

        // Thêm creator làm thành viên
        addMemberToConversation(saved.getId(), creatorId, creatorId, "CREATOR");

        // Thêm các thành viên khác nếu có
        if (req.getMemberIds() != null && !req.getMemberIds().isEmpty()) {
            for (Long memberId : req.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    addMemberToConversation(saved.getId(), memberId, creatorId, "MEMBER");
                }
            }
        }

        return saved;
    }

@Transactional
public Conversation createGroupConversation(ConversationCreateRequest req, Long creatorId) {
    // Kiểm tra số lượng thành viên tối thiểu (bao gồm cả người tạo)
    if (req.getMemberIds() == null || req.getMemberIds().size() < 1) {
        throw new IllegalArgumentException("Nhóm chat cần ít nhất 2 thành viên (bao gồm cả bạn)");
    }
    
    // Tạo danh sách tất cả thành viên (bao gồm người tạo)
    List<Long> allMembers = new ArrayList<>(req.getMemberIds());
    
    // Đảm bảo creator có trong danh sách
    if (!allMembers.contains(creatorId)) {
        allMembers.add(creatorId);
    }
    
    // BỎ KIỂM TRA BẠN BÈ - không cần phải là bạn bè nữa
    
    // Kiểm tra tất cả thành viên có tồn tại không
    for (Long memberId : allMembers) {
        if (!userRepo.existsById(memberId)) {
            throw new IllegalArgumentException("Người dùng không tồn tại: " + memberId);
        }
    }
    
    // Tạo conversation
    Conversation conversation = Conversation.builder()
            .type(ConversationType.GROUP)
            .name(req.getName())
            .description(req.getDescription())
            .createdBy(userRepo.findById(creatorId).orElseThrow(() -> new IllegalArgumentException("User không tồn tại")))
            .isPublic(req.getIsPublic() != null ? req.getIsPublic() : true)
            .maxMembers(req.getMaxMembers() != null ? req.getMaxMembers() : 200)
            .lastActivity(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    
    Conversation savedConv = convRepo.save(conversation);
    
    // Thêm tất cả thành viên
    for (Long memberId : allMembers) {
        String role = memberId.equals(creatorId) ? "CREATOR" : "MEMBER";
        addMemberToConversation(savedConv.getId(), memberId, creatorId, role);
    }
    
    return savedConv;
}
    @Transactional
    public Conversation getOrCreateDirectConversation(Long user1Id, Long user2Id) {
        // Kiểm tra đã có conversation DIRECT chưa
        Optional<Conversation> existingConv = convRepo.findDirectConversation(user1Id, user2Id);
        
        if (existingConv.isPresent()) {
            return existingConv.get();
        }
        
        // Tạo conversation DIRECT mới
        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .name("Direct Chat")
                .createdBy(userRepo.findById(user1Id).orElseThrow(() -> new IllegalArgumentException("User không tồn tại")))
                .isPublic(false)
                .maxMembers(2)
                .lastActivity(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        Conversation savedConv = convRepo.save(conversation);
        
        // Thêm cả 2 user vào conversation
        addMemberToConversation(savedConv.getId(), user1Id, user1Id, "MEMBER");
        addMemberToConversation(savedConv.getId(), user2Id, user1Id, "MEMBER");
        
        return savedConv;
    }

    @Transactional
    public Conversation updateConversation(Long conversationId, ConversationCreateRequest req, Long actorId) {
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));

        if (!isCreatorOrAdmin(conversationId, actorId)) {
            throw new SecurityException("Không có quyền cập nhật");
        }

        if (req.getType() != null) {
            conv.setType(parseTypeOrDefault(req.getType(), conv.getType()));
        }
        if (req.getName() != null) conv.setName(req.getName());
        if (req.getDescription() != null) conv.setDescription(req.getDescription());
        if (req.getIsPublic() != null) conv.setIsPublic(req.getIsPublic());
        if (req.getMaxMembers() != null) conv.setMaxMembers(req.getMaxMembers());

        conv.setUpdatedAt(Instant.now());
        return convRepo.save(conv);
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long actorId) {
        log.info("========== DELETE CONVERSATION START ==========");
        log.info("Conversation ID: {}, Actor ID: {}", conversationId, actorId);
        
        // 1. Tìm conversation
        log.info("1. Looking for conversation {} in database...", conversationId);
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> {
                    log.error("Conversation {} NOT FOUND in database", conversationId);
                    return new IllegalArgumentException("Không tìm thấy cuộc trò chuyện");
                });

        log.info("2. Found conversation: {} (ID: {})", conv.getName(), conv.getId());
        log.info("3. Created by user ID: {}", conv.getCreatedBy().getId());
        
        // 2. Kiểm tra quyền
        log.info("4. Checking permissions for actor {}...", actorId);
        ConversationMemberId id = new ConversationMemberId(conversationId, actorId);
        Optional<ConversationMember> actorMember = memberRepo.findById(id);
        
        boolean isCreator = false;
        if (actorMember.isPresent()) {
            log.info("5. Actor is member with role: {}", actorMember.get().getRole());
            isCreator = "CREATOR".equals(actorMember.get().getRole());
        } else {
            log.info("5. Actor not in member list, checking if is creator...");
            isCreator = conv.getCreatedBy().getId().equals(actorId);
            log.info("   Is actor the creator? {}", isCreator);
        }
        
        if (!isCreator) {
            log.error("6. ERROR: Actor {} is NOT the creator of conversation {}", actorId, conversationId);
            throw new SecurityException("Chỉ người tạo mới có thể xóa nhóm");
        }
        
        log.info("6. Permission check PASSED - Actor is creator");

        try {
            // 3. Đếm số lượng bản ghi liên quan trước khi xóa
            log.info("7. Counting related records before delete...");
            long memberCount = memberRepo.countByIdConversationId(conversationId);
            long messageCount = messageRepo.countByConversationId(conversationId);
            
            log.info("8. Records to delete: Members={}, Messages={}", memberCount, messageCount);
            
            // 4. Xóa theo thứ tự
            log.info("9. Starting deletion process...");
            
            // 4.1 Xóa message status
            log.info("10. Deleting message status...");
            try {
                int deletedStatuses = statusRepo.deleteByConversationId(conversationId);
                log.info("   -> Deleted {} message status records", deletedStatuses);
            } catch (Exception e) {
                log.warn("   -> Could not delete message status: {}", e.getMessage());
                deleteStatusNative(conversationId);
            }
            
            // 4.2 Xóa message attachments
            log.info("11. Deleting message attachments...");
            try {
                int deletedAttachments = attachmentRepo.deleteByConversationId(conversationId);
                log.info("   -> Deleted {} message attachment records", deletedAttachments);
            } catch (Exception e) {
                log.warn("   -> Could not delete message attachments: {}", e.getMessage());
                deleteAttachmentsNative(conversationId);
            }
            
            // 4.3 Xóa messages
            log.info("12. Deleting messages...");
            try {
                int deletedMessages = messageRepo.deleteByConversationId(conversationId);
                log.info("   -> Deleted {} message records", deletedMessages);
            } catch (Exception e) {
                log.error("   -> ERROR deleting messages: {}", e.getMessage());
                deleteMessagesNative(conversationId);
            }
            
            // 4.4 Xóa members
            log.info("13. Deleting members...");
            try {
                int deletedMembers = memberRepo.deleteByConversationId(conversationId);
                log.info("   -> Deleted {} member records", deletedMembers);
            } catch (Exception e) {
                log.error("   -> ERROR deleting members: {}", e.getMessage());
                deleteMembersNative(conversationId);
            }
            
            // 4.5 Xóa conversation
            log.info("14. Deleting conversation entity...");
            convRepo.delete(conv);
            convRepo.flush();
            
            log.info("========== DELETE CONVERSATION SUCCESS ==========");
            log.info("15. Conversation {} and all related data deleted from database", conversationId);
            
        } catch (Exception e) {
            log.error("========== DELETE CONVERSATION FAILED ==========");
            log.error("Error deleting conversation {}: {}", conversationId, e.getMessage(), e);

            log.info("Trying native query delete as fallback...");
            try {
                deleteConversationNative(conversationId);
                log.info("Native delete succeeded");
            } catch (Exception ex) {
                log.error("Native delete also failed: {}", ex.getMessage(), ex);
                throw new RuntimeException("Không thể xóa nhóm: " + e.getMessage(), e);
            }
        }
    }

    private void deleteStatusNative(Long conversationId) {
        try {
            int count = entityManager.createNativeQuery(
                "DELETE FROM message_status WHERE message_id IN (SELECT id FROM chat_message WHERE conversation_id = ?1)")
                .setParameter(1, conversationId)
                .executeUpdate();
            log.info("Native: Deleted {} message_status records", count);
        } catch (Exception e) {
            log.error("Native delete status failed: {}", e.getMessage());
        }
    }
    
    private void deleteAttachmentsNative(Long conversationId) {
        try {
            int count = entityManager.createNativeQuery(
                "DELETE FROM message_attachment WHERE message_id IN (SELECT id FROM chat_message WHERE conversation_id = ?1)")
                .setParameter(1, conversationId)
                .executeUpdate();
            log.info("Native: Deleted {} message_attachment records", count);
        } catch (Exception e) {
            log.error("Native delete attachments failed: {}", e.getMessage());
        }
    }
    
    private void deleteMessagesNative(Long conversationId) {
        try {
            int count = entityManager.createNativeQuery(
                "DELETE FROM chat_message WHERE conversation_id = ?1")
                .setParameter(1, conversationId)
                .executeUpdate();
            log.info("Native: Deleted {} chat_message records", count);
        } catch (Exception e) {
            log.error("Native delete messages failed: {}", e.getMessage());
        }
    }
    
    private void deleteMembersNative(Long conversationId) {
        try {
            int count = entityManager.createNativeQuery(
                "DELETE FROM conversation_member WHERE conversation_id = ?1")
                .setParameter(1, conversationId)
                .executeUpdate();
            log.info("Native: Deleted {} conversation_member records", count);
        } catch (Exception e) {
            log.error("Native delete members failed: {}", e.getMessage());
        }
    }
    
    @Transactional
    public void deleteConversationNative(Long conversationId) {
        log.info("Using NATIVE QUERY to delete conversation {}", conversationId);
        
        try {
            // Xóa bằng native query để tránh constraint issues
            log.info("Deleting related tables with native queries...");
            deleteStatusNative(conversationId);
            deleteAttachmentsNative(conversationId);
            deleteMessagesNative(conversationId);
            deleteMembersNative(conversationId);
            int convCount = entityManager.createNativeQuery(
                "DELETE FROM conversation WHERE id = ?1")
                .setParameter(1, conversationId)
                .executeUpdate();
            log.info("Native: Deleted {} conversation records", convCount);
            
            log.info("NATIVE DELETE SUCCESSFUL for conversation {}", conversationId);
            
        } catch (Exception e) {
            log.error("NATIVE DELETE FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể xóa nhóm bằng native query: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> getConversationOpt(Long id) {
        return convRepo.findById(id);
    }
   @Transactional
public void addMember(Long conversationId, AddMemberRequest req, Long actorId) {
    Conversation conv = convRepo.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));

    AppUser userToAdd = userRepo.findById(req.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng để thêm"));

    if (!isCreatorOrAdmin(conversationId, actorId)) {
        throw new SecurityException("Không được phép thêm thành viên");
    }

    Long max = conv.getMaxMembers() == null ? Long.MAX_VALUE : conv.getMaxMembers().longValue();
    long currentCount = memberRepo.countByIdConversationId(conversationId);
    if (currentCount >= max) {
        throw new IllegalStateException("Đã đầy thành viên");
    }

    ConversationMemberId cmId = new ConversationMemberId(conversationId, userToAdd.getId());
    if (memberRepo.existsById(cmId)) {
        ConversationMember existing = memberRepo.findById(cmId).get();
        if (req.getRole() != null) existing.setRole(req.getRole());
        if (req.getNickname() != null) existing.setNickname(req.getNickname());
        memberRepo.save(existing);
        return;
    }

    addMemberToConversation(conversationId, userToAdd.getId(), actorId, 
                           req.getRole() == null ? "MEMBER" : req.getRole());
}

    @Transactional
    public void removeMember(Long conversationId, Long removeUserId, Long actorId) {
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));

        ConversationMemberId removeId = new ConversationMemberId(conversationId, removeUserId);
        if (!memberRepo.existsById(removeId)) {
            throw new IllegalArgumentException("Không tìm thấy thành viên trong cuộc trò chuyện");
        }

        boolean actorIsSelf = actorId.equals(removeUserId);
        boolean actorIsAdminOrCreator = isCreatorOrAdmin(conversationId, actorId);

        if (!actorIsSelf && !actorIsAdminOrCreator) {
            throw new SecurityException("Không được phép xóa thành viên");
        }

        // Kiểm tra xem có phải là CREATOR không
        Optional<ConversationMember> memberToRemove = memberRepo.findById(removeId);
        if (memberToRemove.isPresent() && "CREATOR".equals(memberToRemove.get().getRole())) {
            throw new IllegalArgumentException("Không thể xóa người tạo nhóm");
        }

        // Kiểm tra số lượng thành viên còn lại
        long remainingCount = memberRepo.countByIdConversationId(conversationId);
        if (remainingCount <= 1) {
            // Nếu chỉ còn 1 thành viên, xóa luôn conversation
            convRepo.delete(conv);
            log.info("Đã xóa conversation vì chỉ còn 1 thành viên");
        } else {
            // Xóa thành viên
            memberRepo.deleteById(removeId);

            conv.setLastActivity(Instant.now());
            convRepo.save(conv);
        }
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(Long id) {
        return convRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));
    }

    @Transactional(readOnly = true)
    public Page<Conversation> listConversationsForMember(Long userId, Pageable pageable) {
        log.debug("Listing conversations for user {}", userId);
        Page<Conversation> result = convRepo.findAllByMember(userId, pageable);
        log.debug("Found {} conversations for user {}", result.getTotalElements(), userId);
        return result;
    }

    @Transactional(readOnly = true)
    public Page<Conversation> search(String keyword, Pageable pageable) {
        throw new UnsupportedOperationException("Chưa hỗ trợ tìm kiếm phân trang");
    }

    @Transactional(readOnly = true)
    public List<Long> getConversationMemberIds(Long conversationId) {
        List<ConversationMember> members = memberRepo.findByIdConversationId(conversationId);
        return members.stream()
                .map(m -> m.getUser().getId())
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isUserInConversation(Long conversationId, Long userId) {
        ConversationMemberId id = new ConversationMemberId(conversationId, userId);
        return memberRepo.existsById(id);
    }

    // Helper method để thêm thành viên
    private void addMemberToConversation(Long conversationId, Long userId, Long actorId, String role) {
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));
        
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        ConversationMemberId cmId = new ConversationMemberId(conversationId, userId);
        
        ConversationMember cm = ConversationMember.builder()
                .id(cmId)
                .conversation(conv)
                .user(user)
                .role(role)
                .joinedAt(Instant.now())
                .nickname(null)
                .muted(false)
                .build();

        memberRepo.save(cm);

        conv.setLastActivity(Instant.now());
        convRepo.save(conv);
    }

    private ConversationType parseTypeOrDefault(String t, ConversationType def) {
        if (t == null) return def;
        try {
            return ConversationType.valueOf(t.trim().toUpperCase());
        } catch (Exception ex) {
            return def;
        }
    }

    private boolean isCreatorOrAdmin(Long conversationId, Long userId) {
        ConversationMemberId id = new ConversationMemberId(conversationId, userId);
        Optional<ConversationMember> m = memberRepo.findById(id);
        return m.isPresent() && ("CREATOR".equals(m.get().getRole()) || "ADMIN".equals(m.get().getRole()));
    }

    private void validateGroupCreation(ConversationCreateRequest req, Long creatorId) {
        if (req.getMemberIds() != null && req.getMemberIds().size() < 2) {
            throw new IllegalArgumentException("Nhóm chat cần ít nhất 3 thành viên (bao gồm cả bạn)");
        }

         if (req.getMemberIds() != null) {
        List<Long> allMembers = new ArrayList<>(req.getMemberIds());
        allMembers.add(creatorId);
        
        for (Long memberId : allMembers) {
            if (!userRepo.existsById(memberId)) {
                throw new IllegalArgumentException("Người dùng không tồn tại: " + memberId);
            }
        }
    }
    }
    
    public boolean areFriends(Long user1Id, Long user2Id) {
        return friendshipService.areFriends(user1Id, user2Id);
    }
}