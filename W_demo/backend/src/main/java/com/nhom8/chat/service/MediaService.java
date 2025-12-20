package com.nhom8.chat.service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nhom8.chat.dto.MediaDTO;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.Media;
import com.nhom8.chat.entity.enums.MediaType;
import com.nhom8.chat.mapper.MediaMapper;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final AppUserRepository appUserRepository;
    private final OpenAIModerationService openAIModerationService;
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục tải lên", e);
        }
    }

    // ========================== CÁC METHOD CŨ - GIỮ NGUYÊN HOÀN TOÀN ==========================

    public Media uploadMedia(Long userId, MultipartFile file, MediaType mediaType, String caption) {
        try {
            AppUser user = appUserRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

            if (file.isEmpty()) {
                throw new IllegalArgumentException("File trống");
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String fileName = UUID.randomUUID().toString() + fileExtension;
            String filePath = this.fileStorageLocation.resolve(fileName).toString();

            Files.copy(file.getInputStream(), Paths.get(filePath));

            Media media = new Media();
            media.setUser(user);
            media.setFileName(originalFileName);
            media.setFilePath(filePath);
            media.setFileUrl("/uploads/" + fileName);
            media.setFileSize(file.getSize());
            media.setMimeType(file.getContentType());
            media.setMediaType(mediaType);
            media.setTemp(false);
            media.setUploadedAt(Instant.now());

            // Lưu caption và visibility ngay từ đầu
            if (caption != null && !caption.isBlank()) {
                media.setCaption(caption.trim());
                media.setVisibility("PUBLIC");
            }
              Media saved = mediaRepository.save(media);
openAIModerationService.moderate(saved);
return saved;


        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file", e);
        }

    }

    public List<Media> getUserMedia(Long userId, MediaType mediaType) {
        if (mediaType != null) {
            return mediaRepository.findByUserIdAndMediaType(userId, mediaType);
        }
        return mediaRepository.findByUserId(userId);
    }

    public void deleteMedia(Long userId, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Không thấy media"));

        if (!media.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("không thể xóa media");
        }

        try {
            Files.deleteIfExists(Paths.get(media.getFilePath()));
            mediaRepository.delete(media);
        } catch (IOException e) {
            throw new RuntimeException("không thể xóa file", e);
        }
    }

    public Media getMedia(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Không thấy media"));
    }

    public Media save(Media media) {
        return mediaRepository.save(media);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }

    // ========================== METHOD MỚI CHO BỘ SƯU TẬP (MY MEDIA GALLERY) ==========================

    /**
     * Lấy danh sách media của chính user (dùng cho bộ sưu tập cá nhân)
     * Hỗ trợ phân trang và lọc theo loại media
     */
    public Page<Media> getMyMedia(Long userId, MediaType mediaType, Pageable pageable) {
        if (mediaType != null) {
            return mediaRepository.findByUserIdAndMediaType(userId, mediaType, pageable);
        }
        return mediaRepository.findByUserId(userId, pageable);
    }

    /**
     * Overload tiện lợi cho client (page + size)
     */
    public Page<Media> getMyMedia(Long userId, MediaType mediaType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        return getMyMedia(userId, mediaType, pageable);
    }

    /**
     * Đếm tổng số media của user (dùng để tính số trang trong gallery)
     */
    public long getMyMediaCount(Long userId, MediaType mediaType) {
        if (mediaType != null) {
            return mediaRepository.countByUserIdAndMediaType(userId, mediaType);
        }
        return mediaRepository.countByUserId(userId);
    }
    public Page<MediaDTO> getPublicFeed(Pageable pageable) {
    return mediaRepository
        .findAllByMediaTypeAndVisibilityAndHiddenFalseOrderByUploadedAtDesc(
            MediaType.PHOTO, "PUBLIC", pageable)
        .map(MediaMapper::toDTO);
}

}