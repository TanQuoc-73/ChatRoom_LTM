// API Base URL Configuration
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081/api';

// API Endpoints
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: `${API_BASE_URL}/auth/login`,
    REGISTER: `${API_BASE_URL}/auth/register`,
    LOGOUT: `${API_BASE_URL}/auth/logout`,
    VALIDATE: `${API_BASE_URL}/auth/validate`,
    FORGOT_PASSWORD: `${API_BASE_URL}/auth/forgot-password`,
    RESET_PASSWORD: `${API_BASE_URL}/auth/reset-password`,
  },
  MESSAGES: {
    BASE: `${API_BASE_URL}/messages`,
    BY_CONVERSATION: (convId: number) => `${API_BASE_URL}/messages/conversation/${convId}`,
    MARK_READ: (msgId: number) => `${API_BASE_URL}/messages/${msgId}/read`,
  },
  CONVERSATIONS: {
    BASE: `${API_BASE_URL}/conversations`,
    BY_ID: (id: number) => `${API_BASE_URL}/conversations/${id}`,
    MEMBERS: (id: number) => `${API_BASE_URL}/conversations/${id}/members`,
  },
  USERS: {
    BASE: `${API_BASE_URL}/users`,
    BY_ID: (id: number) => `${API_BASE_URL}/users/${id}`,
    PROFILE: `${API_BASE_URL}/users/profile`,
  },
} as const;

// Local Storage Keys
export const STORAGE_KEYS = {
  SESSION_TOKEN: 'sessionToken',
  USER_ID: 'userId',
  USERNAME: 'username',
  DISPLAY_NAME: 'displayName',
} as const;
