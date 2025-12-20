package com.nhom8.chat.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhom8.chat.entity.Media;
import com.nhom8.chat.entity.enums.MediaType;

public interface MediaRepository extends JpaRepository<Media, Long> {

    // === CÁC METHOD CŨ - GIỮ NGUYÊN ===
    List<Media> findByUserId(Long userId);

    List<Media> findByUserIdAndMediaType(Long userId, MediaType mediaType);

    List<Media> findByMediaType(MediaType mediaType);

    Page<Media> findAllByMediaTypeAndVisibilityOrderByUploadedAtDesc(
            MediaType mediaType, String visibility, Pageable pageable);

    // === THÊM MỚI CHO BỘ SƯU TẬP (MY MEDIA GALLERY) ===

    // Lấy media của user với phân trang
    Page<Media> findByUserId(Long userId, Pageable pageable);

    // Lấy media của user theo loại + phân trang
    Page<Media> findByUserIdAndMediaType(Long userId, MediaType mediaType, Pageable pageable);

    // Đếm tổng số media của user
    long countByUserId(Long userId);

    // Đếm theo loại media
    long countByUserIdAndMediaType(Long userId, MediaType mediaType);

    Page<Media> findAllByMediaTypeAndVisibilityAndHiddenFalseOrderByUploadedAtDesc(
    MediaType type,
    String visibility,
    Pageable pageable
);
@Query("""
    SELECT COALESCE(SUM(m.violationCount), 0)
    FROM Media m
    WHERE m.user.id = :userId
""")
int sumViolationByUser(@Param("userId") Long userId);

Page<Media> findByHiddenTrue(Pageable pageable);



}