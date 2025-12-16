package com.nhom8.chat.dto;

import java.time.Instant;

import com.nhom8.chat.entity.enums.MediaType;

import lombok.Data;

@Data
public class MediaDTO {
    private Long id;
    private Long userId;

    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private MediaType mediaType;

    private Integer width;
    private Integer height;
    private Integer duration;
    private String thumbnailPath;

    private boolean temp;
    private Instant uploadedAt;

    private String caption;
    private String visibility;
}
