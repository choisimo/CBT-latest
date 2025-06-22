import React, { createContext, useState, useEffect, ReactNode, useCallback, useMemo } from 'react';
import * as Keychain from 'react-native-keychain';
import { BASIC_URL } from '../constants/api';
import { Alert } from 'react-native';
import { createLoginPayload, getLoginErrorMessage } from '../utils/validation';
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
      const refreshRes = await fetch(`${BASIC_URL}/refresh`, {
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
  const signIn = useCallback(async (identifier: string, password: string) => {
    setIsAuthLoading(true);
    try {
      // 유틸리티 함수를 사용하여 안전한 로그인 페이로드 생성
      const loginPayload = createLoginPayload(identifier, password);
      
      console.log('로그인 요청 데이터:', loginPayload);

      const res = await fetch(`${BASIC_URL}/api/public/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginPayload),
      });

      // 응답이 JSON인지 확인
      const contentType = res.headers.get('content-type');
      const isJson = contentType && contentType.includes('application/json');

      if (!res.ok) {
        let errorMessage = '로그인에 실패했습니다';

        if (isJson) {
          try {
            const errorJson = await res.json();
            errorMessage = errorJson.message || errorMessage;
          } catch (jsonError) {
            console.error('JSON 파싱 실패:', jsonError);
          }
        } else {
          // HTML 응답일 경우 상태 코드로 대략적인 메시지 제공
          switch (res.status) {
            case 401:
              errorMessage = '아이디 또는 비밀번호가 올바르지 않습니다';
              break;
            case 404:
              errorMessage = '사용자를 찾을 수 없습니다';
              break;
            case 500:
              errorMessage = '서버 오류가 발생했습니다';
              break;
            default:
              errorMessage = `서버 오류 ${res.status}`;
          }
        }

        Alert.alert('로그인 실패', errorMessage);
        return;
      }

      if (!isJson) {
        Alert.alert('로그인 실패', '서버 응답 형식이 올바르지 않습니다');
        return;
      }

      const json = (await res.json()) as {
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
        const user: User = {
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
        setUser(user);
      } else {
        // 실패 메시지 Alert
        Alert.alert('로그인 실패', json.message || '로그인에 실패했습니다');
      }
    } catch (e: any) {
      // 네트워크 또는 예외 처리
      console.error('Sign-in exception:', e);
      let errorMessage = '로그인 중 오류가 발생했습니다';

      // JSON 파싱 에러인 경우 더 구체적인 메시지
      if (e.message && e.message.includes('JSON')) {
        errorMessage = '서버 응답을 처리하는 중 오류가 발생했습니다';
      }

      Alert.alert('오류', errorMessage);
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

