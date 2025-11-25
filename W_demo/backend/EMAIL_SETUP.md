# Hướng Dẫn Cấu Hình Email Cho Tính Năng Quên Mật Khẩu

## 📧 Cấu Hình Gmail

### Bước 1: Tạo App Password cho Gmail

1. Đăng nhập vào tài khoản Gmail của bạn
2. Truy cập: https://myaccount.google.com/security
3. Bật **2-Step Verification** (nếu chưa bật)
4. Sau khi bật 2FA, tìm mục **App passwords**
5. Chọn **Select app** → **Mail**
6. Chọn **Select device** → **Other (Custom name)**
7. Nhập tên: `ChatRoom Backend`
8. Click **Generate**
9. Copy mã 16 ký tự được tạo ra (ví dụ: `abcd efgh ijkl mnop`)

### Bước 2: Cập Nhật application.properties

Mở file `src/main/resources/application.properties` và thay đổi:

```properties
# Thay your-email@gmail.com bằng email Gmail của bạn
spring.mail.username=your-email@gmail.com

# Thay your-app-password bằng App Password vừa tạo (16 ký tự, không có khoảng trắng)
spring.mail.password=abcdefghijklmnop
```

**Ví dụ:**

```properties
spring.mail.username=tanquoc73@gmail.com
spring.mail.password=xyzw abcd efgh ijkl
```

### Bước 3: Kiểm Tra Cấu Hình

Sau khi cập nhật, khởi động lại ứng dụng và test tính năng quên mật khẩu.

## 🔐 Bảo Mật

⚠️ **QUAN TRỌNG:**

- **KHÔNG** commit file `application.properties` có chứa thông tin email thật lên Git
- Sử dụng environment variables cho production:
  ```properties
  spring.mail.username=${EMAIL_USERNAME}
  spring.mail.password=${EMAIL_PASSWORD}
  ```

## 📝 Sử Dụng Email Khác (Không phải Gmail)

Nếu bạn muốn dùng email provider khác:

### Outlook/Hotmail:

```properties
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=your-email@outlook.com
spring.mail.password=your-password
```

### Yahoo:

```properties
spring.mail.host=smtp.mail.yahoo.com
spring.mail.port=587
spring.mail.username=your-email@yahoo.com
spring.mail.password=your-app-password
```

## 🧪 Test API Endpoints

### 1. Request OTP (Quên mật khẩu)

```bash
POST http://localhost:8081/api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response thành công:**

```json
{
  "success": true,
  "message": "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.",
  "userId": null,
  "username": null,
  "sessionToken": null
}
```

### 2. Reset Password với OTP

```bash
POST http://localhost:8081/api/auth/reset-password
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "newpassword123"
}
```

**Response thành công:**

```json
{
  "success": true,
  "message": "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.",
  "userId": null,
  "username": null,
  "sessionToken": null
}
```

## ⏰ Thời Gian Hết Hạn OTP

- OTP có hiệu lực trong **10 phút**
- Sau khi sử dụng OTP, nó sẽ bị vô hiệu hóa
- Khi request OTP mới, tất cả OTP cũ sẽ bị hủy

## 🛡️ Tính Năng Bảo Mật

1. ✅ OTP 6 chữ số ngẫu nhiên
2. ✅ Hết hạn sau 10 phút
3. ✅ Chỉ sử dụng được 1 lần
4. ✅ Tự động đăng xuất tất cả session sau khi đổi mật khẩu
5. ✅ Gửi email xác nhận sau khi đổi mật khẩu thành công
