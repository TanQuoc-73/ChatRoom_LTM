package com.nhom8.chat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.Media;
import com.nhom8.chat.entity.enums.MediaType;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MediaService {
    private final MediaRepository mediaRepository;
    private final AppUserRepository appUserRepository;
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục tải lên", e);
        }
    }

    public Media uploadMedia(Long userId, MultipartFile file, MediaType mediaType) {
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

            return mediaRepository.save(media);

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

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }

    public Media save(Media media) {
    return mediaRepository.save(media);
}
}