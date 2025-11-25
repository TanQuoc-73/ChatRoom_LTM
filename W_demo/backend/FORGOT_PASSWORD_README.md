# 🔐 Tính Năng Quên Mật Khẩu với OTP

## 📋 Tổng Quan

Tính năng này cho phép người dùng khôi phục mật khẩu thông qua mã OTP (One-Time Password) được gửi qua email.

## ✨ Tính Năng

- ✅ Gửi mã OTP 6 chữ số qua email
- ✅ OTP hết hạn sau 10 phút
- ✅ Mỗi OTP chỉ sử dụng được 1 lần
- ✅ Tự động hủy tất cả OTP cũ khi tạo OTP mới
- ✅ Đăng xuất tất cả phiên đăng nhập sau khi đổi mật khẩu
- ✅ Gửi email xác nhận sau khi đổi mật khẩu thành công

## 🏗️ Kiến Trúc

### 1. Entity

- **PasswordResetToken**: Lưu trữ OTP và thông tin liên quan
  - `id`: ID tự động tăng
  - `user`: Liên kết với AppUser
  - `otp`: Mã OTP 6 chữ số
  - `expiresAt`: Thời gian hết hạn (10 phút)
  - `used`: Trạng thái đã sử dụng
  - `createdAt`, `usedAt`: Timestamps

### 2. Repository

- **PasswordResetTokenRepository**: Quản lý OTP tokens
  - `findByUserAndOtpAndUsedFalseAndExpiresAtAfter()`: Tìm OTP hợp lệ
  - `findByUserAndUsedFalse()`: Tìm tất cả OTP chưa dùng của user
  - `deleteByExpiresAtBefore()`: Xóa OTP đã hết hạn

### 3. Service

- **EmailService**: Gửi email
  - `sendOtpEmail()`: Gửi mã OTP
  - `sendPasswordResetConfirmation()`: Gửi xác nhận đổi mật khẩu
- **AuthService**: Logic nghiệp vụ
  - `requestPasswordReset()`: Tạo và gửi OTP
  - `resetPassword()`: Xác thực OTP và đổi mật khẩu

### 4. Controller

- **AuthController**: API endpoints
  - `POST /api/auth/forgot-password`: Yêu cầu OTP
  - `POST /api/auth/reset-password`: Đặt lại mật khẩu

### 5. DTOs

- **ForgotPasswordRequest**: Request OTP
  ```json
  {
    "email": "user@example.com"
  }
  ```
- **ResetPasswordRequest**: Reset password
  ```json
  {
    "email": "user@example.com",
    "otp": "123456",
    "newPassword": "newpassword123"
  }
  ```

## 🚀 Cài Đặt

### 1. Dependencies (đã thêm vào pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### 2. Cấu Hình Email

Xem file [EMAIL_SETUP.md](./EMAIL_SETUP.md) để biết chi tiết cách cấu hình Gmail.

### 3. Tạo Bảng Database

Khi chạy ứng dụng lần đầu, Hibernate sẽ tự động tạo bảng `password_reset_tokens`:

```sql
CREATE TABLE password_reset_tokens (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL,
    otp VARCHAR(6) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    used_at DATETIME2,
    FOREIGN KEY (user_id) REFERENCES app_users(id)
);
```

## 📡 API Endpoints

### 1. Yêu Cầu OTP

**Endpoint:** `POST /api/auth/forgot-password`

**Request:**

```json
{
  "email": "user@example.com"
}
```

**Response Success (200):**

```json
{
  "success": true,
  "message": "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.",
  "userId": null,
  "username": null,
  "sessionToken": null
}
```

**Response Error (400):**

```json
{
  "success": false,
  "message": "Email không tồn tại trong hệ thống",
  "userId": null,
  "username": null,
  "sessionToken": null
}
```

### 2. Đặt Lại Mật Khẩu

**Endpoint:** `POST /api/auth/reset-password`

**Request:**

```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "newpassword123"
}
```

**Response Success (200):**

```json
{
  "success": true,
  "message": "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.",
  "userId": null,
  "username": null,
  "sessionToken": null
}
```

**Response Error (400):**

```json
{
  "success": false,
  "message": "Mã OTP không hợp lệ hoặc đã hết hạn",
  "userId": null,
  "username": null,
  "sessionToken": null
}
```

## 🔒 Bảo Mật

1. **OTP Ngẫu Nhiên**: Sử dụng `SecureRandom` để tạo OTP
2. **Thời Gian Hết Hạn**: OTP chỉ có hiệu lực 10 phút
3. **Sử Dụng 1 Lần**: OTP bị vô hiệu hóa sau khi sử dụng
4. **Hủy OTP Cũ**: Tất cả OTP cũ bị hủy khi tạo OTP mới
5. **Đăng Xuất Tự Động**: Tất cả session bị đăng xuất sau khi đổi mật khẩu
6. **Email Xác Nhận**: Gửi email thông báo khi mật khẩu được thay đổi

## 🧪 Testing

### Test với Postman/Thunder Client

1. **Request OTP:**

```bash
POST http://localhost:8081/api/auth/forgot-password
Content-Type: application/json

{
  "email": "test@example.com"
}
```

2. **Kiểm tra email** → Lấy mã OTP

3. **Reset Password:**

```bash
POST http://localhost:8081/api/auth/reset-password
Content-Type: application/json

{
  "email": "test@example.com",
  "otp": "123456",
  "newPassword": "newpass123"
}
```

4. **Login với mật khẩu mới:**

```bash
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "newpass123",
  "deviceType": "WEB"
}
```

## 📊 Flow Diagram

```
User                    Backend                 Email Service
  |                        |                          |
  |--forgot-password------>|                          |
  |     (email)            |                          |
  |                        |--Generate OTP            |
  |                        |--Save to DB              |
  |                        |--Send Email------------->|
  |                        |                          |
  |<---OTP sent------------|                          |
  |                        |                          |
  |                        |                    [User checks email]
  |                        |                          |
  |--reset-password------->|                          |
  |  (email, OTP, newPwd)  |                          |
  |                        |--Validate OTP            |
  |                        |--Update Password         |
  |                        |--Mark OTP as used        |
  |                        |--Logout all sessions     |
  |                        |--Send Confirmation------>|
  |                        |                          |
  |<---Success-------------|                          |
```

## 📝 Notes

- OTP được lưu trong database với mã hóa BCrypt cho password
- Email template có thể tùy chỉnh trong `EmailService.java`
- Có thể thêm rate limiting để tránh spam request OTP
- Có thể thêm CAPTCHA để tăng cường bảo mật

## 🐛 Troubleshooting

### Email không được gửi

1. Kiểm tra cấu hình Gmail App Password
2. Kiểm tra firewall/antivirus có block port 587
3. Xem logs để biết lỗi chi tiết

### OTP không hợp lệ

1. Kiểm tra OTP có hết hạn chưa (10 phút)
2. Kiểm tra email có đúng không
3. Kiểm tra OTP đã được sử dụng chưa

## 📚 Tài Liệu Tham Khảo

- [Spring Boot Mail](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
- [JavaMailSender](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/mail/javamail/JavaMailSender.html)
