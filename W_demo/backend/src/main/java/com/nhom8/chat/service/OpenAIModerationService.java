package com.nhom8.chat.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.nhom8.chat.entity.Media;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIModerationService {

    /** ID user hệ thống (admin bot) */
    private static final Long SYSTEM_ADMIN_ID = 6L;

    private final MediaModerationService moderationService;

    /** API key lấy từ application.yml / ENV */
    @Value("${openai.api-key:}")
    private String apiKey;

    private WebClient webClient;
    private boolean openaiEnabled = false;

    /**
     * Init WebClient nếu có API key
     */
    @PostConstruct
    void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ OpenAI API key không tồn tại → bỏ qua AI moderation");
            openaiEnabled = false;
            return;
        }

        webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        openaiEnabled = true;
        log.info("✅ OpenAI Moderation Service initialized");
    }

    /**
     * Entry point: gọi sau khi media được upload
     * Chạy async, KHÔNG block user
     */
    @Async("taskExecutor")
    public void moderate(Media media) {
        if (!openaiEnabled) {
            return;
        }

        try {
            boolean violate = false;

            // 1️⃣ Moderation caption
            if (media.getCaption() != null && !media.getCaption().isBlank()) {
                violate = checkText(media.getCaption());
            }

            // 2️⃣ Moderation image (chỉ khi chưa vi phạm từ text)
            if (!violate
                && media.getMediaType() == com.nhom8.chat.entity.enums.MediaType.PHOTO
                && media.getFileUrl() != null) {

                violate = checkImage(media.getFileUrl());
            }

            // 3️⃣ Nếu vi phạm → ẩn bài + cảnh báo
            if (violate) {
                log.warn("🚨 Media {} bị phát hiện vi phạm (AI)", media.getId());

                moderationService.hideMedia(
                    SYSTEM_ADMIN_ID,
                    media.getId(),
                    "Phát hiện vi phạm tự động bằng AI"
                );
            }

        } catch (Exception e) {
            log.error("❌ OpenAI moderation error for media {}: {}", media.getId(), e.getMessage());
        }
    }

    /**
     * Check text bằng OpenAI Moderation API
     */
    private boolean checkText(String text) {
        try {
            Map<String, Object> body = Map.of(
                "model", "omni-moderation-latest",
                "input", text
            );

            Map<String, Object> response = webClient.post()
                .uri("/moderations")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response == null || !response.containsKey("results")) {
                return false;
            }

            List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.get("results");

            if (results.isEmpty()) {
                return false;
            }

            Boolean flagged = (Boolean) results.get(0).get("flagged");
            return Boolean.TRUE.equals(flagged);

        } catch (Exception e) {
            log.warn("Text moderation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check image bằng GPT-4o-mini (Vision)
     * ⚠️ imageUrl PHẢI là public URL
     */
    private boolean checkImage(String imageUrl) {
        try {
            Map<String, Object> payload = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", List.of(
                            Map.of(
                                "type", "text",
                                "text",
                                "Does this image contain sexual content, violence, hate symbols, or illegal activities? Answer YES or NO only."
                            ),
                            Map.of(
                                "type", "image_url",
                                "image_url", Map.of("url", imageUrl)
                            )
                        )
                    )
                ),
                "max_tokens", 10
            );

            Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response == null || !response.containsKey("choices")) {
                return false;
            }

            List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");

            if (choices.isEmpty()) {
                return false;
            }

            Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

            String content = message.get("content").toString().toLowerCase();

            return content.contains("yes");

        } catch (Exception e) {
            log.warn("Image moderation failed: {}", e.getMessage());
            return false;
        }
    }
}
