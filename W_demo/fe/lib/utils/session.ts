import { STORAGE_KEYS } from '../constants';

/**
 * Session Token Management Utility
 */
export const SessionManager = {
  /**
   * Store session token in localStorage
   */
  setToken: (token: string): void => {
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.SESSION_TOKEN, token);
    }
  },

  /**
   * Retrieve session token from localStorage
   */
  getToken: (): string | null => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem(STORAGE_KEYS.SESSION_TOKEN);
    }
    return null;
  },

  /**
   * Remove session token from localStorage
   */
  removeToken: (): void => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(STORAGE_KEYS.SESSION_TOKEN);
      localStorage.removeItem(STORAGE_KEYS.USER_ID);
      localStorage.removeItem(STORAGE_KEYS.USERNAME);
      localStorage.removeItem(STORAGE_KEYS.DISPLAY_NAME);
    }
  },

  /**
   * Get Authorization header with Bearer token
   */
  getAuthHeader: (): HeadersInit => {
    const token = SessionManager.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  },

  /**
   * Store user info
   */
  setUserInfo: (userId: number, username: string, displayName?: string): void => {
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.USER_ID, userId.toString());
      localStorage.setItem(STORAGE_KEYS.USERNAME, username);
      if (displayName) {
        localStorage.setItem(STORAGE_KEYS.DISPLAY_NAME, displayName);
      }
    }
  },

  /**
   * Get user info
   */
  getUserInfo: (): { userId: number | null; username: string | null; displayName: string | null } => {
    if (typeof window !== 'undefined') {
      const userId = localStorage.getItem(STORAGE_KEYS.USER_ID);
      const username = localStorage.getItem(STORAGE_KEYS.USERNAME);
      const displayName = localStorage.getItem(STORAGE_KEYS.DISPLAY_NAME);
      return {
        userId: userId ? parseInt(userId) : null,
        username,
        displayName,
      };
    }
    return { userId: null, username: null, displayName: null };
  },
};

/**
 * API Error Handler
 */
export const handleApiError = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return 'Đã xảy ra lỗi không xác định';
};

/**
 * Validate Email Format
 */
export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Validate Password Strength
 */
export const isValidPassword = (password: string): { valid: boolean; message?: string } => {
  if (password.length < 6) {
    return { valid: false, message: 'Mật khẩu phải có ít nhất 6 ký tự' };
  }
  return { valid: true };
};
