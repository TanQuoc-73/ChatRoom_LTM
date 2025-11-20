package com.nhom8.chat.repository;

import java.util.List;
import com.nhom8.chat.entity.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom8.chat.entity.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByUserId(Long userId);
    List<Media> findByUserIdAndMediaType(Long userId, MediaType mediaType);
    List<Media> findByMediaType(MediaType mediaType);
}

