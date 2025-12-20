package com.nhom8.chat.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom8.chat.dto.MediaDTO;
import com.nhom8.chat.mapper.MediaMapper;
import com.nhom8.chat.service.AuthService;
import com.nhom8.chat.service.MediaModerationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/moderation/media")
@RequiredArgsConstructor
public class MediaModerationController {

    private final MediaModerationService moderationService;
    private final AuthService authService;

    @PatchMapping("/{mediaId}/hide")
    public ResponseEntity<?> hideMedia(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long mediaId,
            @RequestParam(required = false) String reason) {

        Long actorId = getUserId(auth);
        moderationService.hideMedia(actorId, mediaId, reason);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<?> deleteMedia(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long mediaId) {

        Long actorId = getUserId(auth);
        moderationService.deleteMedia(actorId, mediaId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(String auth) {
        return authService.validateSession(auth.substring(7))
                .orElseThrow().getId();
    }
    @GetMapping("/hidden")
public ResponseEntity<Page<MediaDTO>> getHiddenMedia(
        @RequestHeader("Authorization") String auth,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Long actorId = getUserId(auth);
    Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

    Page<MediaDTO> result = moderationService
            .getHiddenMedia(actorId, pageable)
            .map(MediaMapper::toDTO);

    return ResponseEntity.ok(result);
}

}
