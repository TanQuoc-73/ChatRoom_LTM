package com.nhom8.chat.controller;

import com.nhom8.chat.dto.MediaDTO;
import com.nhom8.chat.entity.enums.MediaType;
import com.nhom8.chat.mapper.MediaMapper;
import com.nhom8.chat.service.AuthService;
import com.nhom8.chat.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final AuthService authService;

    @PostMapping("/upload")
    public ResponseEntity<MediaDTO> uploadMedia(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") MediaType mediaType) {

        Long userId = extractUserId(authorization);

        var saved = mediaService.uploadMedia(userId, file, mediaType);

        return ResponseEntity.ok(MediaMapper.toDTO(saved));
    }

    @GetMapping("/my-media")
    public ResponseEntity<List<MediaDTO>> getMyMedia(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) MediaType mediaType) {

        Long userId = extractUserId(authorization);

        var mediaList = mediaService.getUserMedia(userId, mediaType);

        return ResponseEntity.ok(mediaList.stream()
                .map(MediaMapper::toDTO)
                .toList());
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaDTO> getMedia(@PathVariable Long mediaId) {
        var media = mediaService.getMedia(mediaId);
        return ResponseEntity.ok(MediaMapper.toDTO(media));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<?> deleteMedia(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long mediaId) {

        Long userId = extractUserId(authorization);
        mediaService.deleteMedia(userId, mediaId);

        return ResponseEntity.ok().build();
    }

    private Long extractUserId(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);

            var user = authService.validateSession(token);
            if (user.isPresent()) return user.get().getId();
        }
        throw new IllegalArgumentException("token ko hợp lệ òi");
    }
}
