package com.nhom8.chat.controller;

import com.nhom8.chat.dto.MessageRequest;
import com.nhom8.chat.dto.MessageResponse;
import com.nhom8.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService msgService;

    @PostMapping
    public ResponseEntity<MessageResponse> send(
            @Validated @RequestBody MessageRequest req,
            Principal principal
    ) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(msgService.sendMessage(userId, req));
    }

    @GetMapping("/conversation/{convId}")
    public ResponseEntity<Page<MessageResponse>> getMessagesPaged(
            @PathVariable Long convId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(
                msgService.getMessagesPaged(convId, PageRequest.of(page, size))
        );
    }

    @GetMapping("/conversation/{convId}/list")
    public ResponseEntity<List<MessageResponse>> getMessagesList(
            @PathVariable Long convId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(
                msgService.getMessagesPaged(convId, PageRequest.of(page, size)).getContent()
        );
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            Principal principal
    ) {
        Long userId = extractUserId(principal);
        msgService.markRead(id, userId);
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(Principal principal) {
        return Long.parseLong(principal.getName());
    }
}
