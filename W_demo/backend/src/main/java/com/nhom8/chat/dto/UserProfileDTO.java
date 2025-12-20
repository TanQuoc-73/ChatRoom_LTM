package com.nhom8.chat.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.nhom8.chat.entity.enums.Gender;
import com.nhom8.chat.entity.enums.UserRole;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileDTO {
    private Long userId;
    private String username;
    private String email;
    private String displayName;
    private String firstName;
    private String lastName;
    private String bio;
    private String website;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Boolean verified;
    private Instant lastActive;
    private String avatarUrl;
    private String coverUrl;
     private UserRole role;
}