# Frontend Architecture

## 📁 Cấu Trúc Thư Mục

```
fe/
├── app/                      # Next.js App Router
│   ├── page.tsx             # Trang đăng nhập/quên mật khẩu
│   ├── layout.tsx           # Root layout
│   └── globals.css          # Global styles
│
├── types/                    # TypeScript Type Definitions
│   └── index.ts             # Tất cả interfaces và types
│
├── services/                 # API Services
│   └── auth.service.ts      # Authentication API calls
│
├── hooks/                    # Custom React Hooks
│   └── useAuth.ts           # Authentication hook
│
├── lib/                      # Utilities & Configuration
│   ├── constants.ts         # API endpoints, storage keys
│   └── utils/
│       └── session.ts       # Session management, validation
│
└── components/               # Reusable Components
    └── (future components)
```

## 🎯 Phân Chia Rõ Ràng

### 1. **types/** - Type Definitions

**Mục đích**: Định nghĩa tất cả TypeScript interfaces và types

**Nội dung**:

- `LoginRequest`, `RegisterRequest`
- `ForgotPasswordRequest`, `ResetPasswordRequest`
- `AuthResponse`
- `User`, `Message`, `Conversation`

**Khi nào sử dụng**: Import types khi cần type-checking

```typescript
import type { AuthResponse, LoginRequest } from "@/types";
```

---

### 2. **services/** - API Services

**Mục đích**: Xử lý tất cả API calls đến backend

**Nội dung**:

- `auth.service.ts`: Login, logout, forgot password, reset password
- (Future) `message.service.ts`, `conversation.service.ts`

**Đặc điểm**:

- Pure functions, không có React hooks
- Trả về Promises
- Xử lý errors và trả về structured responses

**Ví dụ**:

```typescript
import { AuthService } from "@/services/auth.service";

const result = await AuthService.login(username, password);
```

---

### 3. **hooks/** - Custom React Hooks

**Mục đích**: Business logic và state management cho components

**Nội dung**:

- `useAuth.ts`: Authentication logic với loading, error states
- (Future) `useMessages.ts`, `useConversations.ts`

**Đặc điểm**:

- Sử dụng React hooks (useState, useEffect, etc.)
- Gọi services để fetch data
- Quản lý loading và error states
- Validation logic

**Ví dụ**:

```typescript
import { useAuth } from "@/hooks/useAuth";

const { isLoading, error, login } = useAuth();
const success = await login(username, password);
```

---

### 4. **lib/** - Utilities & Configuration

#### **lib/constants.ts**

**Mục đích**: Configuration và constants

**Nội dung**:

- `API_BASE_URL`
- `API_ENDPOINTS` (tất cả endpoint URLs)
- `STORAGE_KEYS` (localStorage keys)

#### **lib/utils/session.ts**

**Mục đích**: Helper functions

**Nội dung**:

- `SessionManager`: localStorage operations
- `handleApiError`: Error handling
- `isValidEmail`, `isValidPassword`: Validation

**Ví dụ**:

```typescript
import { SessionManager } from "@/lib/utils/session";

SessionManager.setToken(token);
const token = SessionManager.getToken();
```

---

## 🔄 Luồng Dữ Liệu

```
Component (page.tsx)
    ↓ uses
Custom Hook (useAuth)
    ↓ calls
Service (auth.service)
    ↓ uses
Constants (API_ENDPOINTS)
    ↓ calls
Backend API
```

## 📝 Ví Dụ Sử Dụng

### Login Flow

```typescript
// 1. Component sử dụng hook
const { isLoading, error, login } = useAuth();

// 2. User submit form
const handleSubmit = async () => {
  const success = await login(username, password);
  if (success) {
    // Redirect to chat
  }
};

// 3. Hook gọi service
const login = async (username, password) => {
  const result = await AuthService.login(username, password);
  // Handle result
};

// 4. Service gọi API
export const AuthService = {
  login: async (username, password) => {
    const response = await fetch(API_ENDPOINTS.AUTH.LOGIN, {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
    return await response.json();
  },
};
```

## 🎨 Best Practices

1. **Types**: Luôn import types với `type` keyword

   ```typescript
   import type { User } from "@/types";
   ```

2. **Services**: Không sử dụng React hooks trong services
3. **Hooks**: Tất cả business logic nên ở hooks, không ở components

4. **Constants**: Không hardcode URLs hoặc keys, dùng constants

5. **Error Handling**: Luôn handle errors ở hook level

## 🚀 Mở Rộng Trong Tương Lai

Khi thêm features mới, follow pattern này:

1. **Thêm types** trong `types/index.ts`
2. **Tạo service** trong `services/`
3. **Tạo hook** trong `hooks/`
4. **Sử dụng hook** trong component

Ví dụ cho Messages feature:

- `types/index.ts` → Add `Message`, `MessageRequest`
- `services/message.service.ts` → Create `MessageService`
- `hooks/useMessages.ts` → Create `useMessages` hook
- `app/chat/page.tsx` → Use `useMessages()`
