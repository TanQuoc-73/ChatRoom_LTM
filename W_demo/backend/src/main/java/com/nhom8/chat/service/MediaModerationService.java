package com.nhom8.chat.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.Media;
import com.nhom8.chat.entity.Notification;
import com.nhom8.chat.entity.enums.NotificationType;
import com.nhom8.chat.entity.enums.UserRole;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.MediaRepository;
import com.nhom8.chat.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaModerationService {

    private final MediaRepository mediaRepo;
    private final AppUserRepository userRepo;
    private final NotificationRepository notificationRepo;

    private static final int MAX_VIOLATION = 5;

    public void hideMedia(Long actorId, Long mediaId, String reason) {
        AppUser actor = userRepo.findById(actorId).orElseThrow();

        if (actor.getRole() == UserRole.USER) {
            throw new SecurityException("Không có quyền kiểm duyệt");
        }

        Media media = mediaRepo.findById(mediaId).orElseThrow();

        if (Boolean.TRUE.equals(media.getHidden())) {
            return;
        }

        media.setHidden(true);
        media.setViolationCount(media.getViolationCount() + 1);
        mediaRepo.save(media);

        notifyUser(actor, media, reason);
        checkAndBlockUser(userRepo.findById(media.getUser().getId()).orElseThrow());
    }

    public void deleteMedia(Long actorId, Long mediaId) {
        AppUser actor = userRepo.findById(actorId).orElseThrow();

        if (actor.getRole() == UserRole.USER) {
            throw new SecurityException("Không có quyền");
        }

        mediaRepo.deleteById(mediaId);
    }

    private void notifyUser(AppUser actor, Media media, String reason) {
        notificationRepo.save(
            Notification.builder()
                .user(media.getUser())
                .sender(actor)
                .type(NotificationType.SYSTEM)
                .title("Cảnh báo vi phạm")
                .content(
                    "Bài đăng của bạn vi phạm tiêu chuẩn cộng đồng"
                    + (reason != null ? ": " + reason : "")
                )
                .relatedEntityType("MEDIA")
                .relatedEntityId(media.getId())
                .read(false)
                .createdAt(Instant.now())
                .build()
        );
    }

    private void checkAndBlockUser(AppUser user) {
        int total = mediaRepo.sumViolationByUser(user.getId());

        if (total >= MAX_VIOLATION && Boolean.TRUE.equals(user.getActive())) {
            user.setActive(false);
            userRepo.save(user);

            notificationRepo.save(
                Notification.builder()
                    .user(user)
                    .type(NotificationType.SYSTEM)
                    .title("Tài khoản bị khóa")
                    .content("Tài khoản đã bị khóa do vi phạm nhiều lần")
                    .read(false)
                    .createdAt(Instant.now())
                    .build()
            );
        }
    }
    public Page<Media> getHiddenMedia(Long actorId, Pageable pageable) {
    AppUser actor = userRepo.findById(actorId).orElseThrow();

    if (actor.getRole() == UserRole.USER) {
        throw new SecurityException("Không có quyền");
    }

    return mediaRepo.findByHiddenTrue(pageable);
}

}
