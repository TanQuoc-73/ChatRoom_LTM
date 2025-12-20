package com.nhom8.chat.dto;

import lombok.Data;

@Data
public class PostCreateRequest {
    private String author;
    private String imageUrl;
    private String caption;
}