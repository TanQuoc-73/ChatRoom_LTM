package com.nhom8.chat.controller;

import com.nhom8.chat.dto.MediaDTO;
import com.nhom8.chat.entity.enums.MediaType;
import com.nhom8.chat.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feed") // Đường dẫn cho feed
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép frontend truy cập
public class FeedController {

    private final MediaService mediaService;

    /**
     * API LẤY FEED: Lấy tất cả các ảnh công khai, phân trang.
     * Frontend sẽ gọi API này.
     */
    @GetMapping
    public ResponseEntity<Page<MediaDTO>> getPublicFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Lấy các media là ảnh, có trạng thái công khai, sắp xếp theo thời gian mới nhất
        Pageable pageable = PageRequest.of(page, size);
        Page<MediaDTO> feedPage = mediaService.getPublicFeed(pageable);
        return ResponseEntity.ok(feedPage);
    }
}