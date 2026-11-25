import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { api, bindTokenStore } from "../api/client";
import type { AuthResponse, UserProfile } from "../api/types";

interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, displayName: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const REFRESH_STORAGE_KEY = "riftradar_refresh_token";
const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const accessTokenRef = useRef<string | null>(null);
  const refreshTokenRef = useRef<string | null>(sessionStorage.getItem(REFRESH_STORAGE_KEY));

  const applyAuth = useCallback((auth: AuthResponse) => {
    accessTokenRef.current = auth.accessToken;
    refreshTokenRef.current = auth.refreshToken;
    sessionStorage.setItem(REFRESH_STORAGE_KEY, auth.refreshToken);
    setUser(auth.user);
  }, []);

  const clearSession = useCallback(() => {
    accessTokenRef.current = null;
    refreshTokenRef.current = null;
    sessionStorage.removeItem(REFRESH_STORAGE_KEY);
    setUser(null);
  }, []);

  useEffect(() => {
    bindTokenStore({
      getAccessToken: () => accessTokenRef.current,
      getRefreshToken: () => refreshTokenRef.current,
      onTokensUpdated: applyAuth,
      onSessionExpired: clearSession,
    });

    const restore = async () => {
      if (!refreshTokenRef.current) {
        setIsLoading(false);
        return;
      }
      try {
        const profile = await api.me();
        setUser(profile);
      } catch {
        clearSession();
      } finally {
        setIsLoading(false);
      }
    };
    void restore();
  }, [applyAuth, clearSession]);

  const login = useCallback(async (email: string, password: string) => {
    const auth = await api.login(email, password);
    applyAuth(auth);
  }, [applyAuth]);

  const register = useCallback(async (email: string, displayName: string, password: string) => {
    const auth = await api.register(email, displayName, password);
    applyAuth(auth);
  }, [applyAuth]);

  const logout = useCallback(async () => {
    try {
      if (accessTokenRef.current) {
        await api.logout();
      }
    } finally {
      clearSession();
    }
  }, [clearSession]);

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: user !== null,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return ctx;
}
