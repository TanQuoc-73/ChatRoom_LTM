package com.nhom8.chat.dto;
import java.time.LocalDate;

import com.nhom8.chat.entity.enums.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 64, message = "Tên người dùng phải từ 3 đến 64 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email phải hợp lệ")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Mật khẩu phải ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng nhập tên hiển thị")
    @Size(max = 128, message = "Tên hiển thị không được vượt quá 128 ký tự")
    private String displayName;

    @NotBlank(message = "Bắt buộc phải nhập tên")
    @Size(max = 64, message = "Tên không được vượt quá 64 ký tự")
    private String firstName;

    @NotBlank(message = "Bắt buộc phải nhập họ")
    @Size(max = 64, message = "Họ không được vượt quá 64 ký tự")
    private String lastName;

    // THÊM 2 TRƯỜNG NÀY
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;
    private Gender gender;
}