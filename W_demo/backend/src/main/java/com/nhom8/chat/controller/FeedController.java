package com.nhom8.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom8.chat.dto.MediaDTO;
import com.nhom8.chat.entity.enums.MediaType;
import com.nhom8.chat.mapper.MediaMapper;
import com.nhom8.chat.service.AuthService;
import com.nhom8.chat.service.MediaService;

import lombok.RequiredArgsConstructor;

// controller cung cấp dữ liệu feed media
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final MediaService mediaService;
    private final AuthService authService;

    // lấy danh sách media hiển thị feed
    @GetMapping
    public ResponseEntity<List<MediaDTO>> getFeed(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MediaType mediaType
    ) {
        Long userId = extractUserId(authorization);

        var mediaList = mediaService.getUserMedia(userId, mediaType);

        return ResponseEntity.ok(
                mediaList.stream()
                        .map(MediaMapper::toDTO)
                        .toList()
        );
    }

    // lấy user id từ token trong header
    private Long extractUserId(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            var user = authService.validateSession(token);
            if (user.isPresent()) return user.get().getId();
        }
        throw new IllegalArgumentException("token ko hợp lệ òi");
    }
}
