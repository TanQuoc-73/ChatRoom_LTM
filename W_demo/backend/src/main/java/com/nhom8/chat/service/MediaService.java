package com.nhom8.chat.service;

import com.nhom8.chat.entity.Media;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.enums.MediaType;
import com.nhom8.chat.repository.MediaRepository;
import com.nhom8.chat.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MediaService {
    private final MediaRepository mediaRepository;
    private final AppUserRepository appUserRepository;
    
    // Thư mục lưu file - có thể config trong application.properties
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    // Sử dụng @PostConstruct để khởi tạo thư mục
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    // Upload media file
    public Media uploadMedia(Long userId, MultipartFile file, MediaType mediaType) {
        try {
            AppUser user = appUserRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // Generate unique filename
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String fileName = UUID.randomUUID().toString() + fileExtension;
            String filePath = this.fileStorageLocation.resolve(fileName).toString();

            // Save file to disk
            Files.copy(file.getInputStream(), Paths.get(filePath));

            // Create media record
            Media media = new Media();
            media.setUser(user);
            media.setFileName(originalFileName);
            media.setFilePath(filePath);
            media.setFileUrl("/uploads/" + fileName); // URL để truy cập file
            media.setFileSize(file.getSize());
            media.setMimeType(file.getContentType());
            media.setMediaType(mediaType);
            media.setTemp(false);
            media.setUploadedAt(Instant.now());

            return mediaRepository.save(media);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    // Lấy danh sách media của user
    public List<Media> getUserMedia(Long userId, MediaType mediaType) {
        if (mediaType != null) {
            return mediaRepository.findByUserIdAndMediaType(userId, mediaType);
        }
        return mediaRepository.findByUserId(userId);
    }

    // Xóa media
    public void deleteMedia(Long userId, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));

        // Kiểm tra quyền sở hữu
        if (!media.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this media");
        }

        try {
            // Xóa file từ disk
            Files.deleteIfExists(Paths.get(media.getFilePath()));
            // Xóa record từ database
            mediaRepository.delete(media);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    // Lấy media by ID
    public Media getMedia(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }
}