// src/main/java/com/nhom8/chat/exception/BusinessException.java
package com.nhom8.chat.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}