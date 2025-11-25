package com.nhom8.chat.dto;

import com.nhom8.chat.entity.enums.DeviceType;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Vui lòng nhập username")
    private String username;

    @NotBlank(message = "Vui lòng nhập pass")
    private String password;

    private DeviceType deviceType = DeviceType.WEB;
}