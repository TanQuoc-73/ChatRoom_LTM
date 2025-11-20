package com.nhom8.chat.controller;

import com.nhom8.chat.dto.*;
import com.nhom8.chat.entity.Conversation;
import com.nhom8.chat.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService convService;

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
}
