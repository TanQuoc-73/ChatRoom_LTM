# Frontend Authentication - Vanilla HTML/CSS/JavaScript

Đây là phiên bản frontend thuần (vanilla) cho hệ thống xác thực, bao gồm đăng nhập, đăng ký và quên mật khẩu.

## Cấu trúc thư mục

```
frontend/
├── index.html          # Trang chính với form xác thực
├── css/
│   └── styles.css      # CSS với glassmorphism và backdrop blur
├── js/
│   ├── config.js       # Cấu hình API endpoints
│   ├── utils.js        # Các hàm tiện ích (validation, session management)
│   ├── auth-service.js # Service xử lý API calls
│   └── app.js          # Logic chính của ứng dụng
├── src/                # Thư mục chứa assets (videos, images)
└── README.md           # File này
```

## Tính năng

### 1. Đăng nhập

- Nhập tên đăng nhập và mật khẩu
- Lưu session token vào localStorage
- Chuyển hướng đến trang home sau khi đăng nhập thành công

### 2. Đăng ký

- Nhập thông tin: username, email, password, display name, first name, last name
- Validation email và mật khẩu
- Chuyển về trang đăng nhập sau khi đăng ký thành công

### 3. Quên mật khẩu

- Nhập email để nhận OTP
- Nhập OTP và mật khẩu mới
- Đặt lại mật khẩu và chuyển về trang đăng nhập

## Cách sử dụng

### 1. Cấu hình API

Mở file `config.js` và cập nhật `API_BASE_URL` nếu cần:

```javascript
const API_BASE_URL = "http://localhost:8081/api";
```

### 2. Chạy ứng dụng

Mở file `index.html` trong trình duyệt web. Bạn có thể:

- Sử dụng Live Server trong VS Code
- Mở trực tiếp file trong trình duyệt
- Sử dụng bất kỳ web server nào (Python, Node.js, etc.)

#### Sử dụng Python HTTP Server:

```bash
# Python 3
python -m http.server 8080

# Sau đó mở: http://localhost:8080
```

#### Sử dụng Node.js http-server:

```bash
npx http-server -p 8080

# Sau đó mở: http://localhost:8080
```

### 3. Video nền (tùy chọn)

Để sử dụng video nền, đảm bảo đường dẫn trong `index.html` đúng:

```html
<source src="../fe/public/videos/background.mp4" type="video/mp4" />
```

Hoặc thay thế bằng đường dẫn video của bạn.

## Thiết kế

### Glassmorphism Effect

- Background blur với `backdrop-filter: blur(40px)`
- Semi-transparent backgrounds
- Border với opacity thấp
- Drop shadows cho depth

### Responsive Design

- Mobile-first approach
- Breakpoints cho tablet và desktop
- Flexible layouts với flexbox và grid

### Color Scheme

- Primary: Blue gradient (#3B82F6 → #2563EB)
- Success: Green (#22C55E)
- Error: Red (#EF4444)
- Background: Video hoặc image với overlay

## API Endpoints

Ứng dụng sử dụng các endpoints sau:

- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/logout` - Đăng xuất
- `POST /api/auth/forgot-password` - Gửi OTP
- `POST /api/auth/reset-password` - Đặt lại mật khẩu
- `GET /api/auth/validate` - Validate session

## Session Management

Session được quản lý qua localStorage:

- `sessionToken` - JWT token
- `userId` - ID người dùng
- `username` - Tên đăng nhập
- `displayName` - Tên hiển thị

## Validation

### Email

```javascript
function isValidPassword(password) {
  // Thêm logic validation của bạn
}
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

**Lưu ý:** Backdrop filter có thể không hoạt động trên một số trình duyệt cũ.

## Troubleshooting

### CORS Issues

Nếu gặp lỗi CORS, đảm bảo backend đã cấu hình CORS cho phép origin của frontend.

### LocalStorage không hoạt động

Đảm bảo bạn đang chạy ứng dụng qua HTTP/HTTPS, không phải `file://` protocol.

### Video không phát

- Kiểm tra đường dẫn video
- Đảm bảo video format được hỗ trợ (MP4 recommended)
- Fallback image sẽ hiển thị nếu video không load được

## License

MIT
