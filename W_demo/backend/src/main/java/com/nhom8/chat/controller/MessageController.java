package com.nhom8.chat.controller;

import com.nhom8.chat.dto.MessageRequest;
import com.nhom8.chat.dto.MessageResponse;
import com.nhom8.chat.realtime.ChatRealtimeHttpBridge;
import com.nhom8.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// controller xử lý nhắn tin (REST API)
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService msgService;
    private final ChatRealtimeHttpBridge chatRealtimeBridge;

    // gửi tin nhắn mới
    @PostMapping
    public ResponseEntity<MessageResponse> send(
            @RequestHeader("X-USER-ID") Long userId,
            @Validated @RequestBody MessageRequest req
    ) {
        return ResponseEntity.ok(
                msgService.sendMessage(userId, req)
        );
    }

    // lấy tin nhắn theo phân trang
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

    // lấy danh sách tin nhắn + join realtime vào cuộc trò chuyện
    @GetMapping("/conversation/{convId}/list")
    public ResponseEntity<List<MessageResponse>> getMessagesList(
            @PathVariable Long convId,
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        // thông báo realtime: user tham gia cuộc trò chuyện
        chatRealtimeBridge.userJoinConversation(userId, convId);

        return ResponseEntity.ok(
                msgService
                        .getMessagesPaged(convId, PageRequest.of(page, size))
                        .getContent()
        );
    }

    // rời khỏi cuộc trò chuyện (realtime)
    @PostMapping("/conversation/{convId}/leave")
    public ResponseEntity<Void> leaveConversation(
            @PathVariable Long convId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        chatRealtimeBridge.userLeaveConversation(userId, convId);
        return ResponseEntity.ok().build();
    }

    // đánh dấu tin nhắn đã đọc
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        msgService.markRead(id, userId);
        return ResponseEntity.ok().build();
    }
}
