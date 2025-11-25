"use client";
import React, { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { useAuth as useAuthHook } from "@/hooks/useAuth";
import { SessionManager } from "@/lib/utils/session";

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
  useEffect(() => {
    if (token) {
      const userInfo = SessionManager.getUserInfo();
      if (userInfo.username) {
        setUser({
          username: userInfo.username,
          displayName: userInfo.displayName || undefined,
        });
      } else {
        setUser(null);
      }
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
