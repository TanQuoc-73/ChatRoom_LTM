import { API_ENDPOINTS } from '../lib/constants';
import { SessionManager, handleApiError } from '../lib/utils/session';
import type {
  LoginRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  AuthResponse,
} from '@/types';

/**
 * Authentication Service
 * Handles all authentication-related API calls
 */
export const AuthService = {
  /**
   * Login user with username and password
   */
  login: async (username: string, password: string): Promise<AuthResponse> => {
    try {
      const response = await fetch(API_ENDPOINTS.AUTH.LOGIN, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username,
          password,
          deviceType: 'WEB',
        } as LoginRequest),
      });

      const data: AuthResponse = await response.json();

      if (data.success && data.sessionToken) {
        SessionManager.setToken(data.sessionToken);
        if (data.userId && data.username) {
          SessionManager.setUserInfo(data.userId, data.username, data.displayName);
        }
      }

      return data;
    } catch (error) {
      return {
        success: false,
        message: handleApiError(error),
      };
    }
  },

  /**
   * Register new user
   */
  register: async (
    username: string,
    email: string,
    password: string,
    displayName?: string,
    firstName?: string,
    lastName?: string
  ): Promise<AuthResponse> => {
    try {
      const response = await fetch(API_ENDPOINTS.AUTH.REGISTER, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username,
          email,
          password,
          displayName,
          firstName,
          lastName,
        }),
      });

      const data: AuthResponse = await response.json();
      return data;
    } catch (error) {
      return {
        success: false,
        message: handleApiError(error),
      };
    }
  },

  /**
   * Logout current user
   */
  logout: async (): Promise<void> => {
    const token = SessionManager.getToken();
    if (token) {
      try {
        await fetch(API_ENDPOINTS.AUTH.LOGOUT, {
          method: 'POST',
          headers: {
            ...SessionManager.getAuthHeader(),
          },
        });
      } catch (error) {
        console.error('Logout error:', error);
      } finally {
        SessionManager.removeToken();
      }
    }
  },

  /**
   * Send OTP to email for password reset
   */
  forgotPassword: async (email: string): Promise<AuthResponse> => {
    try {
      const response = await fetch(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email } as ForgotPasswordRequest),
      });

      return await response.json();
    } catch (error) {
      return {
        success: false,
        message: handleApiError(error),
      };
    }
  },

  /**
   * Reset password with OTP
   */
  resetPassword: async (
    email: string,
    otp: string,
    newPassword: string
  ): Promise<AuthResponse> => {
    try {
      const response = await fetch(API_ENDPOINTS.AUTH.RESET_PASSWORD, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          otp,
          newPassword,
        } as ResetPasswordRequest),
      });

      return await response.json();
    } catch (error) {
      return {
        success: false,
        message: handleApiError(error),
      };
    }
  },

  /**
   * Validate current session token
   */
  validateSession: async (): Promise<AuthResponse | null> => {
    const token = SessionManager.getToken();
    if (!token) return null;

    try {
      const response = await fetch(API_ENDPOINTS.AUTH.VALIDATE, {
        method: 'GET',
        headers: {
          ...SessionManager.getAuthHeader(),
        },
      });

      return await response.json();
    } catch (error) {
      SessionManager.removeToken();
      return null;
    }
  },
};
