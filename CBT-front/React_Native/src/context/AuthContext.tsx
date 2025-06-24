import React, { createContext, useState, useEffect, ReactNode, useCallback, useMemo } from 'react';
import * as Keychain from 'react-native-keychain';
import { BASIC_URL } from '../constants/api';
import { Alert } from 'react-native';
import { createLoginPayload, getLoginErrorMessage } from '../utils/validation';
import { safeApiCall } from '../utils/apiHealth';
export type User = {
  loginId: string;
  nickname: string;
  emailVerified: boolean; // 이메일 인증 여부 추가
  email: string;
  userId?: string; // 백엔드에서 오는 userId 필드 추가
  username?: string; // 백엔드에서 오는 username 필드 추가
  roles?: string[]; // 백엔드에서 오는 roles 필드 추가
};

export type AuthContextType = {
  userToken: string | null;
  user: User | null;
  // 앱 기동 시 토큰 확인 + 사용자 조회 전용 로딩
  isBootstrapping: boolean;
  // 로그인/로그아웃/리프레시 등 인증 액션 전용 로딩
  isAuthLoading: boolean;
  signIn: (loginId: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  fetchWithAuth: (url: string, options?: RequestInit) => Promise<Response>;
  refreshUser: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextType>({
  userToken: null,
  isBootstrapping: true,
  isAuthLoading: false,
  user: null,
  signIn: async () => { },
  signOut: async () => { },
  fetchWithAuth: async () => new Response(null, { status: 500 }),
  refreshUser: async () => { },
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [userToken, setUserToken] = useState<string | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [isAuthLoading, setIsAuthLoading] = useState(false);
  const [user, setUser] = useState<User | null>(null);

  // 공통 인증 요청 함수
  const fetchWithAuth = useCallback(async (url: string, options: RequestInit = {}) => {
    const token = userToken;
    console.log(`[fetchWithAuth] Requesting URL: ${url}`);
    console.log(`[fetchWithAuth] Current Token: ${token ? `...${token.slice(-10)}` : 'None'}`);

    const makeRequest = async (t: string) =>
      fetch(url, {
        ...options,
        headers: {
          ...(options.headers || {}),
          Authorization: `Bearer ${t}`,
          'Content-Type': 'application/json',
        },
      });

    if (!token) {
      console.error('[fetchWithAuth] No token available. Aborting.');
      return new Response(JSON.stringify({ message: 'No authentication token available.' }), { status: 401 });
    }

    let res = await makeRequest(token);
    console.log(`[fetchWithAuth] Initial response for ${url}: ${res.status}`);

    if (res.status === 401) {
      console.warn('[fetchWithAuth] Received 401. Attempting to refresh token...');
      const refreshRes = await fetch(`${BASIC_URL}/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ expiredToken: token, provider: 'server' }),
      });
      console.log(`[fetchWithAuth] Refresh endpoint response: ${refreshRes.status}`);

      if (refreshRes.ok) {
        const { access_token: newToken } = await refreshRes.json();
        console.log('[fetchWithAuth] Token refresh successful. Retrying original request...');
        await Keychain.setGenericPassword('authToken', newToken);
        setUserToken(newToken);
        res = await makeRequest(newToken);
        console.log(`[fetchWithAuth] Retried request response for ${url}: ${res.status}`);
      } else {
        console.error('[fetchWithAuth] Token refresh failed. Logging out.');
        await Keychain.resetGenericPassword();
        setUserToken(null);
        setUser(null);
        // Return the failed refresh response to indicate a hard failure
        return refreshRes;
      }
    }
    return res;
  }, [userToken]);

  // 로그인 함수
  const signIn = useCallback(async (identifier: string, password: string) => {
    setIsAuthLoading(true);
    try {
      // 디버깅: 입력 값 확인
      console.log('signIn 함수 호출됨 - identifier:', JSON.stringify(identifier), 'password length:', password?.length);
      
      // 유틸리티 함수를 사용하여 안전한 로그인 페이로드 생성
      const loginPayload = createLoginPayload(identifier, password);
      
      console.log('로그인 요청 데이터:', loginPayload);

      // safeApiCall을 사용하여 안전한 API 호출
      const result = await safeApiCall(
        `${BASIC_URL}/api/public/login`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(loginPayload),
        }
      );

      if (!result.success) {
        Alert.alert('로그인 실패', result.error);
        return;
      }

      const json = result.data as {
        status: string;
        message: string;
        data?: {
          access_token: string;
          user: {
            userId?: string;
            username?: string;
            email: string;
            roles?: string[];
          };
        };
      };

      if (json.status === 'success' && json.data) {
        // 백엔드 응답을 프론트엔드 User 타입에 맞게 변환
        const loginUser: User = {
          loginId: json.data.user.userId || '', // userId를 loginId로 매핑
          nickname: json.data.user.username || '', // username을 nickname으로 매핑
          email: json.data.user.email,
          emailVerified: true, // 로그인 성공시 기본값으로 true 설정
          userId: json.data.user.userId,
          username: json.data.user.username,
          roles: json.data.user.roles,
        };

        // 3) 토큰 저장 & 상태 업데이트
        await Keychain.setGenericPassword('authToken', json.data.access_token);
        setUserToken(json.data.access_token);
        setUser(loginUser);
      } else {
        // 실패 메시지 Alert
        Alert.alert('로그인 실패', json.message || '로그인에 실패했습니다');
      }
    } catch (e: any) {
      // 네트워크 또는 예외 처리
      console.error('Sign-in exception:', e);
      const errorMessage = getLoginErrorMessage(e);
      Alert.alert('로그인 실패', errorMessage);
    } finally {
      setIsAuthLoading(false);
    }
  },
    []
  );

  // 로그아웃 함수
  const signOut = useCallback(async () => {
    setIsAuthLoading(true);
    try {
      // Keychain 에 저장된 토큰 완전 삭제
      await Keychain.resetGenericPassword();
      // 앱 상태 클리어
      setUserToken(null);
      setUser(null);
    } finally {
      // 언제나 로딩 해제
      setIsAuthLoading(false);
    }
  }, []);

  // 앱 시작 시 자동 로그인 부트스트랩
  useEffect(() => {
    const bootstrapAsync = async () => {
      setIsBootstrapping(true);
      let token: string | null = null;

      try {
        const credentials = await Keychain.getGenericPassword();
        if (credentials) {
          token = credentials.password;
        }
      } catch (e) {
        console.error('Keychain access error during bootstrap:', e);
        setIsBootstrapping(false);
        return;
      }

      if (token) {
        const res = await fetch(`${BASIC_URL}/api/users/me`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (res.ok) {
          const json = await res.json();
          setUserToken(token);
          setUser(json.data?.user || null);
        } else if (res.status === 401) {
          const refreshRes = await fetch(`${BASIC_URL}/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ expiredToken: token, provider: 'server' }),
          });

          if (refreshRes.ok) {
            const { access_token: newToken } = await refreshRes.json();
            await Keychain.setGenericPassword('authToken', newToken);
            setUserToken(newToken);
            const res2 = await fetch(`${BASIC_URL}/api/users/me`, {
              headers: { Authorization: `Bearer ${newToken}` },
            });
            if (res2.ok) {
              const json2 = await res2.json();
              setUser(json2.data?.user || null);
            } else {
              await Keychain.resetGenericPassword();
              setUserToken(null);
              setUser(null);
            }
          } else {
            await Keychain.resetGenericPassword();
            setUserToken(null);
            setUser(null);
          }
        }
      }
      setIsBootstrapping(false);
    };

    bootstrapAsync();
  }, []);

  const refreshUser = useCallback(async () => {
    if (!userToken) return;
    const res = await fetchWithAuth(`${BASIC_URL}/api/users/me`);
    if (res.ok) {
      const json = await res.json();
      setUser(json.data?.user || null);
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

