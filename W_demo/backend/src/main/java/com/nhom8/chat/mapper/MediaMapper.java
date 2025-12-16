package com.nhom8.chat.mapper;

import com.nhom8.chat.dto.MediaDTO;
import com.nhom8.chat.entity.Media;

public class MediaMapper {

    public static MediaDTO toDTO(Media media) {
        if (media == null) return null;

        MediaDTO dto = new MediaDTO();
        dto.setId(media.getId());
        dto.setUserId(media.getUser() != null ? media.getUser().getId() : null);

        dto.setFileName(media.getFileName());
        dto.setFilePath(media.getFilePath());

 String fileUrl = media.getFileUrl();
    if (fileUrl == null || fileUrl.isBlank()) {
        // không có gì thì tự build
        fileUrl = "/api/uploads/" + media.getFileName();
    } else {
        // nếu đang ở dạng "/uploads/xxx" thì thêm /api vào
        if (!fileUrl.startsWith("/api/")) {
            // đảm bảo có dấu /
            if (!fileUrl.startsWith("/")) {
                fileUrl = "/" + fileUrl;
            }
            fileUrl = "/api" + fileUrl;   // -> /api/uploads/xxx
        }
    }
    dto.setFileUrl(fileUrl);
 
        dto.setFileSize(media.getFileSize());
        dto.setMimeType(media.getMimeType());
        dto.setMediaType(media.getMediaType());

        dto.setWidth(media.getWidth());
        dto.setHeight(media.getHeight());
        dto.setDuration(media.getDuration());
        dto.setThumbnailPath(media.getThumbnailPath());
        dto.setTemp(media.isTemp());
        dto.setUploadedAt(media.getUploadedAt());
        dto.setCaption(media.getCaption());
        dto.setVisibility(media.getVisibility());

        return dto;
    }
}
