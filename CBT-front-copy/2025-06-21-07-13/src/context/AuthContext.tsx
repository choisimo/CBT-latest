import React, { createContext, useState, useEffect, ReactNode, useCallback, useMemo } from 'react';
import * as Keychain from 'react-native-keychain';
import { BASIC_URL } from '../constants/api';

export type User = {
  userId: string;
  nickname: string;
  role: string;
  emailVerified: boolean; // 이메일 인증 여부 추가
  email: string;
  provider: string;
};

export type AuthContextType = {
  userToken: string | null;
  user: User | null;
  // 앱 기동 시 토큰 확인 + 사용자 조회 전용 로딩
  isBootstrapping: boolean;
  // 로그인/로그아웃/리프레시 등 인증 액션 전용 로딩
  isAuthLoading: boolean;
  signIn: (userId: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  fetchWithAuth: (url: string, options?: RequestInit) => Promise<Response>;
  refreshUser: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextType>({
  userToken: null,
  isBootstrapping: true,
  isAuthLoading: false,
  user: null,
  signIn: async () => {},
  signOut: async () => {},
  fetchWithAuth: async () => new Response(null, { status: 500 }),
  refreshUser: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [userToken, setUserToken] = useState<string | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [isAuthLoading, setIsAuthLoading] = useState(false);
  const [user, setUser] = useState<User | null>(null);

  // 공통 인증 요청 함수
  const fetchWithAuth = useCallback(async (url: string, options: RequestInit = {}) => {
    const token = userToken;
    const makeRequest = async (t: string) =>
      fetch(url, {
        ...options,
        headers: {
          ...(options.headers || {}),
          Authorization: `Bearer ${t}`,
          'Content-Type': 'application/json',
        },
      });

    let res = token ? await makeRequest(token) : new Response(null, { status: 401 });
    if (res.status === 401) {
      const refreshRes = await fetch(`https://${BASIC_URL}/auth/api/protected/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ expiredToken: token, provider: 'server' }),
      });
      if (refreshRes.ok) {
        const { access_token } = await refreshRes.json();
        await Keychain.setGenericPassword('authToken', access_token);
        setUserToken(access_token);
        res = await makeRequest(access_token);
      } else {
        await Keychain.resetGenericPassword();
        setUserToken(null);
        setUser(null);
        return new Response(null, { status: 401 });
      }
    }
    return res;
  }, [userToken]);

  // 로그인 함수
  const signIn = useCallback(async (userId: string, password: string) => {
    setIsAuthLoading(true);
    try {
      const res = await fetch(`https://${BASIC_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId, password }),
      });
      if (res.ok) {
        const { access_token, user } = await res.json();
        await Keychain.setGenericPassword('authToken', access_token);
        setUserToken(access_token);
        setUser(user);
      } else {
        throw new Error(`Login failed: ${res.status}`);
      }
    } finally {
      setIsAuthLoading(false);
    }
  }, []);

  // 로그아웃 함수
  const signOut = useCallback(async () => {
    setIsAuthLoading(true);
    try {
      await fetchWithAuth(`https://${BASIC_URL}/api/public/clean/userTokenCookie`, {
        method: 'POST',
      });
      await Keychain.resetGenericPassword();
      setUserToken(null);
      setUser(null);
    } finally {
      setIsAuthLoading(false);
    }
  }, [fetchWithAuth]);

  // 앱 시작 시 자동 로그인 부트스트랩
  useEffect(() => {
    const bootstrapAsync = async () => {
      try {
        const credentials = await Keychain.getGenericPassword();
        if (credentials) {
          setUserToken(credentials.password);
          const res = await fetchWithAuth(`https://${BASIC_URL}/api/users/me`);
          if (res.ok) {
            const { user } = await res.json();
            setUser(user);
          }
        }
      } catch (e) {
        console.warn('앱 부트스트랩 중 오류:', e);
      } finally {
        setIsBootstrapping(false);
      }
    };
    bootstrapAsync();
  }, [fetchWithAuth]);

  const refreshUser = useCallback(async () => {
    if (!userToken) return;
    const res = await fetchWithAuth(`https://${BASIC_URL}/api/users/me`);
    if (res.ok) {
      const { user } = await res.json();
      setUser(user);
    }
  }, [fetchWithAuth, userToken]);

  const contextValue = useMemo(
    () => ({ userToken, user, isBootstrapping, isAuthLoading, signIn, signOut, fetchWithAuth, refreshUser }),
    [userToken, user, isBootstrapping, isAuthLoading, signIn, signOut, fetchWithAuth, refreshUser]
  );

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

