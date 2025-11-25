import { useState } from 'react';
import { AuthService } from '@/services/auth.service';
import { isValidEmail, isValidPassword } from '@/lib/utils/session';

interface UseAuthReturn {
  isLoading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<boolean>;
  register: (username: string, email: string, password: string, displayName?: string, firstName?: string, lastName?: string) => Promise<boolean>;
  forgotPassword: (email: string) => Promise<boolean>;
  resetPassword: (email: string, otp: string, newPassword: string) => Promise<boolean>;
  logout: () => Promise<void>;
  clearError: () => void;
}

/**
 * Custom hook for authentication operations
 */
export const useAuth = (): UseAuthReturn => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const clearError = () => setError(null);

  const login = async (username: string, password: string): Promise<boolean> => {
    setIsLoading(true);
    setError(null);

    try {
      const result = await AuthService.login(username, password);
      
      if (result.success) {
        return true;
      } else {
        setError(result.message);
        return false;
      }
    } catch (err) {
      setError('Đã xảy ra lỗi khi đăng nhập');
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (
    username: string,
    email: string,
    password: string,
    displayName?: string,
    firstName?: string,
    lastName?: string
  ): Promise<boolean> => {
    if (!isValidEmail(email)) {
      setError('Email không hợp lệ');
      return false;
    }

    const passwordValidation = isValidPassword(password);
    if (!passwordValidation.valid) {
      setError(passwordValidation.message || 'Mật khẩu không hợp lệ');
      return false;
    }

    setIsLoading(true);
    setError(null);

    try {
      const result = await AuthService.register(username, email, password, displayName, firstName, lastName);
      
      if (result.success) {
        return true;
      } else {
        setError(result.message);
        return false;
      }
    } catch (err) {
      setError('Đã xảy ra lỗi khi đăng ký');
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  const forgotPassword = async (email: string): Promise<boolean> => {
    if (!isValidEmail(email)) {
      setError('Email không hợp lệ');
      return false;
    }

    setIsLoading(true);
    setError(null);

    try {
      const result = await AuthService.forgotPassword(email);
      
      if (result.success) {
        return true;
      } else {
        setError(result.message);
        return false;
      }
    } catch (err) {
      setError('Đã xảy ra lỗi khi gửi OTP');
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  const resetPassword = async (
    email: string,
    otp: string,
    newPassword: string
  ): Promise<boolean> => {
    const passwordValidation = isValidPassword(newPassword);
    if (!passwordValidation.valid) {
      setError(passwordValidation.message || 'Mật khẩu không hợp lệ');
      return false;
    }

    setIsLoading(true);
    setError(null);

    try {
      const result = await AuthService.resetPassword(email, otp, newPassword);
      
      if (result.success) {
        return true;
      } else {
        setError(result.message);
        return false;
      }
    } catch (err) {
      setError('Đã xảy ra lỗi khi đặt lại mật khẩu');
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    setIsLoading(true);
    try {
      await AuthService.logout();
    } finally {
      setIsLoading(false);
    }
  };

  return {
    isLoading,
    error,
    login,
    register,
    forgotPassword,
    resetPassword,
    logout,
    clearError,
  };
};
