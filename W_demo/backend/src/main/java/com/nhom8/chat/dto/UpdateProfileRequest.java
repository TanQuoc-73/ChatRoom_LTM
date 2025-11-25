package com.nhom8.chat.dto;

import com.nhom8.chat.entity.enums.Gender;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    @Size(max = 128, message = "Display name quá dài")
    private String displayName;

    @Size(max = 64, message = "First name quá dài")
    private String firstName;

    @Size(max = 64, message = "Last name quá dài")
    private String lastName;

    @Size(max = 500, message = "Bio quá dài")
    private String bio;

    @Size(max = 255, message = "Website quá dài")
    private String website;

    private Gender gender;
    private LocalDate dateOfBirth;
}