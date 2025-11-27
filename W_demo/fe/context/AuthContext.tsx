"use client";
import React, { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { useAuth as useAuthHook } from "@/hooks/useAuth";
import { SessionManager } from "@/lib/utils/session";
import { AuthService } from "@/services/auth.service";

type User = {
  username: string;
  displayName?: string;
  // add more fields if needed (email, avatar, ...)
};

type AuthContextProps = {
  user: User | null;
  setUser: (user: User | null) => void;
  token: string | null;
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextProps | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const { token, login, logout, clearError } = useAuthHook();
  const [user, setUser] = useState<User | null>(null);

  // Update user info when token changes (fetch from session)
  // Update user info when token changes (fetch from session)
  useEffect(() => {
    if (token) {
      // 1. Initial load from local storage
      const userInfo = SessionManager.getUserInfo();
      if (userInfo.username) {
        setUser({
          username: userInfo.username,
          displayName: userInfo.displayName || undefined,
        });
      }

      // 2. Validate session to get fresh data (including displayName if missing)
      AuthService.validateSession().then((response) => {
        if (response && response.success && response.username) {
          // SessionManager is already updated by validateSession
          const freshUser = SessionManager.getUserInfo();
          setUser({
            username: freshUser.username!,
            displayName: freshUser.displayName || undefined,
          });
        } else if (!response) {
          // Token invalid
          setUser(null);
        }
      });
    } else {
      setUser(null);
    }
  }, [token]);

  const wrappedLogin = async (username: string, password: string) => {
    const ok = await login(username, password);
    // token is set inside useAuthHook; useEffect will update user
    return ok;
  };

  const wrappedLogout = async () => {
    await logout();
    setUser(null);
    clearError();
  };

  return (
    <AuthContext.Provider
      value={{ user, setUser, token, login: wrappedLogin, logout: wrappedLogout }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
