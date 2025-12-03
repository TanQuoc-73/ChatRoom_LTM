package com.nhom8.chat.service;

import com.nhom8.chat.dto.MessageRequest;
import com.nhom8.chat.dto.MessageResponse;
import com.nhom8.chat.entity.*;
import com.nhom8.chat.realtime.ChatRealtimeBridge;
import com.nhom8.chat.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatMessageRepository msgRepo;
    private final MessageAttachmentRepository attachRepo;
    private final MessageStatusRepository statusRepo;
    private final ConversationRepository convRepo;
    private final ConversationMemberRepository memberRepo;
    private final MediaRepository mediaRepo;
    private final AppUserRepository userRepo;
    private final ChatRealtimeBridge realtimeBridge;
    private final SimpMessagingTemplate messagingTemplate; // 👈 dùng constructor injection luôn

    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest req) {

        if (req == null || req.getConversationId() == null) {
            throw new IllegalArgumentException("Yêu cầu tin nhắn không hợp lệ");
        }

        AppUser sender = userRepo.findById(senderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người gửi"));
        Conversation conv = convRepo.findById(req.getConversationId())
                .orElseThrow(() -> new NoSuchElementException("Không thấy cuộc trò chuyện"));

        if (req.getClientCid() != null && !req.getClientCid().isBlank()) {
            Optional<ChatMessage> existed = msgRepo.findBySenderIdAndClientCid(senderId, req.getClientCid());
            if (existed.isPresent()) {
                return mapToResponse(existed.get());
            }
        }

        ChatMessage msg = ChatMessage.builder()
                .conversation(conv)
                .sender(sender)
                .content(req.getContent())
                .messageType(req.getMessageType())
                .clientCid(req.getClientCid())
                .sentAt(Instant.now())
                .updatedAt(Instant.now())
                .edited(false)
                .deleted(false)
                .build();

        ChatMessage saved = msgRepo.save(msg);

        List<Long> attachmentIds = new ArrayList<>();
        if (req.getAttachmentMediaIds() != null && !req.getAttachmentMediaIds().isEmpty()) {
            for (Long mediaId : req.getAttachmentMediaIds()) {
                Media m = mediaRepo.findById(mediaId)
                        .orElseThrow(() -> new NoSuchElementException("Không thấy media " + mediaId));
                MessageAttachment att = new MessageAttachment();
                att.setMessage(saved);
                att.setMedia(m);
                att.setCaption(null);
                att.setSortOrder(0);
                attachRepo.save(att);
                attachmentIds.add(att.getId());
            }
        }

        List<ConversationMember> members = memberRepo.findByIdConversationId(req.getConversationId());
        if (members != null && !members.isEmpty()) {
            List<MessageStatus> statuses = members.stream().map(cm -> {
                MessageStatus s = new MessageStatus();
                s.setMessage(saved);
                s.setUser(cm.getUser());
                return s;
            }).collect(Collectors.toList());
            statusRepo.saveAll(statuses);
        }

        conv.setLastMessageId(saved.getId());
        conv.setLastActivity(Instant.now());
        convRepo.save(conv);

        MessageResponse resp = MessageResponse.builder()
                .id(saved.getId())
                .conversationId(conv.getId())
                .senderId(senderId)
                .content(saved.getContent())
                .messageType(saved.getMessageType())
                .sentAt(saved.getSentAt())
                .attachmentIds(attachmentIds)
                .clientCid(saved.getClientCid())
                .isEdited(saved.isEdited())
                .build();

        // 🔥 1) Đẩy sang ChatRealtime TCP (JavaFX sau này dùng)
        try {
            realtimeBridge.broadcastMessage(saved, req.getClientCid());
        } catch (Exception ex) {
            System.err.println("lỗi realtime TCP  " + ex.getMessage());
        }

        // 🔥 2) Đẩy WebSocket cho web UI test
        try {
            messagingTemplate.convertAndSend(
                    "/topic/conversations/" + conv.getId(),
                    resp
            );
        } catch (Exception e) {
            System.err.println("WebSocket send error: " + e.getMessage());
        }

        return resp;
    }

   
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesPaged(Long conversationId, Pageable pageable) {
        Page<ChatMessage> page = msgRepo.findByConversationIdOrderBySentAtDesc(conversationId, pageable);
        return page.map(this::mapToResponse);
    }

    @Transactional
    public void markRead(Long messageId, Long userId) {
        MessageStatusId id = new MessageStatusId(messageId, userId);
        Optional<MessageStatus> ms = statusRepo.findById(id);
        if (ms.isPresent()) {
            MessageStatus s = ms.get();
            s.setReadAt(Instant.now());
            statusRepo.save(s);
            try {
                realtimeBridge.notifyRead(messageId, userId);
            } catch (Exception ex) {
                System.err.println("Realtime đọc không thành công: " + ex.getMessage());
            }
        } else {
            throw new NoSuchElementException("Không tìm thấy MessageStatus");
        }
    }



    private MessageResponse mapToResponse(ChatMessage m) {
        List<Long> attachmentIds = Optional.ofNullable(m.getAttachments())
                .map(list -> list.stream().map(a -> a.getMedia().getId()).collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation() != null ? m.getConversation().getId() : null)
                .senderId(m.getSender() != null ? m.getSender().getId() : null)
                .content(m.getContent())
                .messageType(m.getMessageType())
                .sentAt(m.getSentAt())
                .attachmentIds(attachmentIds)
                .clientCid(m.getClientCid())
                .isEdited(m.isEdited())
                .build();
    }
}
