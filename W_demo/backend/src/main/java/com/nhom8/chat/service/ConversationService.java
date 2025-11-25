package com.nhom8.chat.service;

import com.nhom8.chat.dto.AddMemberRequest;
import com.nhom8.chat.dto.ConversationCreateRequest;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.Conversation;
import com.nhom8.chat.entity.ConversationMember;
import com.nhom8.chat.entity.ConversationMemberId;
import com.nhom8.chat.entity.enums.ConversationType;
import com.nhom8.chat.repository.ConversationMemberRepository;
import com.nhom8.chat.repository.ConversationRepository;
import com.nhom8.chat.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * ConversationService - quản lý tạo/sửa/xóa conversation và quản lý thành viên
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository convRepo;
    private final ConversationMemberRepository memberRepo;
    private final AppUserRepository userRepo;

    /**
     * Tạo conversation mới. Creator sẽ được thêm làm CREATOR member.
     *
     * @param req creator request (type as String, name, description, isPublic, maxMembers)
     * @param creatorId id người tạo
     * @return saved Conversation
     */
    @Transactional
    public Conversation createConversation(ConversationCreateRequest req, Long creatorId) {
        AppUser creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found"));

        ConversationType typeEnum = parseTypeOrDefault(req.getType(), ConversationType.GROUP);

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

        // add creator as CREATOR
        ConversationMember cm = ConversationMember.builder()
                .id(new ConversationMemberId(saved.getId(), creator.getId()))
                .conversation(saved)
                .user(creator)
                .role("CREATOR")
                .joinedAt(Instant.now())
                .nickname(null)
                .muted(false)
                .build();

        memberRepo.save(cm);

        return saved;
    }

    /**
     * Update conversation (name/description/isPublic/maxMembers/type)
     * Only CREATOR or ADMIN can update.
     */
    @Transactional
    public Conversation updateConversation(Long conversationId, ConversationCreateRequest req, Long actorId) {
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // permission check
        if (!isCreatorOrAdmin(conversationId, actorId)) {
            throw new SecurityException("Not allowed to update conversation");
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

    /**
     * Delete conversation - only CREATOR can delete.
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long actorId) {
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // only creator can delete
        ConversationMemberId id = new ConversationMemberId(conversationId, actorId);
        Optional<ConversationMember> actorMember = memberRepo.findById(id);
        if (actorMember.isEmpty() || !"CREATOR".equals(actorMember.get().getRole())) {
            throw new SecurityException("Only creator can delete conversation");
        }

        convRepo.delete(conv);
    }

    /**
     * Add member into conversation. Actor must be CREATOR or ADMIN.
     */
    @Transactional
    public void addMember(Long conversationId, AddMemberRequest req, Long actorId) {
        // basic validation
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        AppUser userToAdd = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User to add not found"));

        // permission: actor must be in room and admin/creator
        if (!isCreatorOrAdmin(conversationId, actorId)) {
            throw new SecurityException("Not allowed to add members");
        }

        // check max members
        Long max = conv.getMaxMembers() == null ? Long.MAX_VALUE : conv.getMaxMembers().longValue();
        long currentCount = memberRepo.countByIdConversationId(conversationId);
        if (currentCount >= max) {
            throw new IllegalStateException("Conversation is full");
        }

        // if already member, ignore or throw
        ConversationMemberId cmId = new ConversationMemberId(conversationId, userToAdd.getId());
        if (memberRepo.existsById(cmId)) {
            // already member - update role/nickname if provided
            ConversationMember existing = memberRepo.findById(cmId).get();
            if (req.getRole() != null) existing.setRole(req.getRole());
            if (req.getNickname() != null) existing.setNickname(req.getNickname());
            memberRepo.save(existing);
            return;
        }

        ConversationMember cm = ConversationMember.builder()
                .id(cmId)
                .conversation(conv)
                .user(userToAdd)
                .role(req.getRole() == null ? "MEMBER" : req.getRole())
                .joinedAt(Instant.now())
                .nickname(req.getNickname())
                .muted(false)
                .build();

        memberRepo.save(cm);

        // update last activity on conversation
        conv.setLastActivity(Instant.now());
        convRepo.save(conv);
    }

    /**
     * Remove member. Actor must be CREATOR or ADMIN or the member himself.
     */
    @Transactional
    public void removeMember(Long conversationId, Long removeUserId, Long actorId) {
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        ConversationMemberId removeId = new ConversationMemberId(conversationId, removeUserId);
        if (!memberRepo.existsById(removeId)) {
            throw new IllegalArgumentException("Member not found in conversation");
        }

        // actor allowed if CREATOR or ADMIN or removing self
        boolean actorIsSelf = actorId.equals(removeUserId);
        boolean actorIsAdminOrCreator = isCreatorOrAdmin(conversationId, actorId);

        if (!actorIsSelf && !actorIsAdminOrCreator) {
            throw new SecurityException("Not allowed to remove member");
        }

        memberRepo.deleteById(removeId);

        // update conversation lastActivity
        conv.setLastActivity(Instant.now());
        convRepo.save(conv);
    }

    /**
     * Get conversation by id (readonly)
     */
    @Transactional(readOnly = true)
    public Conversation getConversation(Long id) {
        return convRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
    }

    /**
     * List conversations for a member (paged)
     */
    @Transactional(readOnly = true)
    public Page<Conversation> listConversationsForMember(Long userId, Pageable pageable) {
        return convRepo.findAllByMember(userId, pageable);
    }

    /**
     * Search conversations by keyword
     */
    @Transactional(readOnly = true)
    public Page<Conversation> search(String keyword, Pageable pageable) {
        // simple approach: reuse searchConversations (list) and convert to page manually is possible,
        // but we already have findBy...; for simplicity return a page via repository search (if implemented).
        // Here fallback: use repository.searchConversations and create a Page manually if needed.
        throw new UnsupportedOperationException("Use repository.searchConversations(...) or implement paging search");
    }

    // -------------------------
    // Helper methods
    // -------------------------
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
}
