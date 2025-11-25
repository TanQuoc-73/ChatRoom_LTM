package com.nhom8.chat.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddMemberRequest {
    @NotNull private Long userId;
    private String role = "MEMBER";
    private String nickname;
}
