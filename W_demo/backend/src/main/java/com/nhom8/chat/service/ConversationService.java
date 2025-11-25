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

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository convRepo;
    private final ConversationMemberRepository memberRepo;
    private final AppUserRepository userRepo;

    @Transactional
    public Conversation createConversation(ConversationCreateRequest req, Long creatorId) {
        AppUser creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Ko thấy người dùng"));

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

    @Transactional
    public Conversation updateConversation(Long conversationId, ConversationCreateRequest req, Long actorId) {
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));

        if (!isCreatorOrAdmin(conversationId, actorId)) {
            throw new SecurityException("ko update nổi");
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
        Conversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));

        ConversationMemberId id = new ConversationMemberId(conversationId, actorId);
        Optional<ConversationMember> actorMember = memberRepo.findById(id);
        if (actorMember.isEmpty() || !"CREATOR".equals(actorMember.get().getRole())) {
            throw new SecurityException("Chỉ người tạo mới có thể xóa");
        }

        convRepo.delete(conv);
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
            throw new IllegalStateException("Full òi");
        }

        ConversationMemberId cmId = new ConversationMemberId(conversationId, userToAdd.getId());
        if (memberRepo.existsById(cmId)) {
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

        conv.setLastActivity(Instant.now());
        convRepo.save(conv);
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

        memberRepo.deleteById(removeId);

        conv.setLastActivity(Instant.now());
        convRepo.save(conv);
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(Long id) {
        return convRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));
    }

    @Transactional(readOnly = true)
    public Page<Conversation> listConversationsForMember(Long userId, Pageable pageable) {
        return convRepo.findAllByMember(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Conversation> search(String keyword, Pageable pageable) {
        throw new UnsupportedOperationException("thử tìm kiêm phân trang");
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
}
