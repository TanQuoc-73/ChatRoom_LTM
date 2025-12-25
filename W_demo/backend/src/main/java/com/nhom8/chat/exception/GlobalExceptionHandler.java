package com.nhom8.chat.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Xử lý các lỗi logic nghiệp vụ từ Service.
     * Chúng ta sẽ phân loại lỗi dựa trên thông điệp (message) của exception.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Business logic error: {}", ex.getMessage());

        String message = ex.getMessage();
        HttpStatus status;

        // Phân loại lỗi dựa vào từ khóa trong message
        if (message.contains("Không tìm thấy")) {
            status = HttpStatus.NOT_FOUND; // 404
        } else if (message.contains("không có quyền") || message.contains("chính mình") || message.contains("quản trị viên khác")) {
            status = HttpStatus.FORBIDDEN; // 403
        } else {
            status = HttpStatus.BAD_REQUEST; // 400 - Cho các lỗi không xác định khác
        }

        return ResponseEntity.status(status)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "message", message
                ));
    }

    /**
     * Xử lý các exception không được bắt ở trên (lưới an toàn cuối cùng).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", "Internal Server Error",
                        "message", "Đã có lỗi xảy ra, vui lòng thử lại sau."
                ));
    }
}