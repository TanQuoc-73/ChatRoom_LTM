package com.nhom8.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RealtimeResponse {
    private boolean success;
    private String message;
    private Object data;
    private String errorCode;
    
    public static RealtimeResponse success(String message, Object data) {
        return RealtimeResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    public static RealtimeResponse error(String message, String errorCode) {
        return RealtimeResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
