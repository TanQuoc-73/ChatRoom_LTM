package com.nhom8.chat.mapper;

import com.nhom8.chat.entity.Media;
import com.nhom8.chat.dto.MediaDTO;

public class MediaMapper {

    public static MediaDTO toDTO(Media media) {
        if (media == null) return null;

        MediaDTO dto = new MediaDTO();
        dto.setId(media.getId());
        dto.setUserId(media.getUser() != null ? media.getUser().getId() : null);

        dto.setFileName(media.getFileName());
        dto.setFilePath(media.getFilePath());
        dto.setFileUrl(media.getFileUrl());
        dto.setFileSize(media.getFileSize());
        dto.setMimeType(media.getMimeType());
        dto.setMediaType(media.getMediaType());

        dto.setWidth(media.getWidth());
        dto.setHeight(media.getHeight());
        dto.setDuration(media.getDuration());
        dto.setThumbnailPath(media.getThumbnailPath());
        dto.setTemp(media.isTemp());
        dto.setUploadedAt(media.getUploadedAt());

        return dto;
    }
}
