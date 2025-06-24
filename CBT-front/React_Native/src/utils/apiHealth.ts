// src/utils/apiHealth.ts
import { BASIC_URL } from '../constants/api';

// Constants for better maintainability
const HEALTH_CHECK_TIMEOUT = 5000; // 5 seconds
const RESPONSE_TEXT_PREVIEW_LENGTH = 500;
const HEALTH_CHECK_DUMMY_LOGIN_ID = 'health_check_dummy';

// Error types for better error handling
enum ApiErrorType {
  SERVER_UNREACHABLE = 'SERVER_UNREACHABLE',
  INVALID_RESPONSE_FORMAT = 'INVALID_RESPONSE_FORMAT',
  TIMEOUT = 'TIMEOUT',
  NETWORK_ERROR = 'NETWORK_ERROR',
  JSON_PARSE_ERROR = 'JSON_PARSE_ERROR',
  SERVER_ERROR = 'SERVER_ERROR',
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

interface ApiResponse {
  success: boolean;
  data?: any;
  error?: string;
  errorType?: ApiErrorType;
}

/**
 * 서버 상태 확인 함수
 * @returns Promise<boolean> 서버가 정상적으로 응답하는지 여부
 */
export const checkServerHealth = async (): Promise<boolean> => {
  const controller = new AbortController();
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  try {
    timeoutId = setTimeout(() => controller.abort(), HEALTH_CHECK_TIMEOUT);

    // 원격 서버의 간단한 Health 체크 - 인증이 필요없는 엔드포인트로 변경
    const response = await fetch(`${BASIC_URL}/api/public/check/loginId/IsDuplicate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ loginId: HEALTH_CHECK_DUMMY_LOGIN_ID }),
      signal: controller.signal,
    });

    // 4xx, 5xx 에러라도 서버가 응답했다면 서버는 살아있는 것으로 판단
    return true;
  } catch (error) {
    console.warn('서버 상태 확인 실패:', error);
    return false;
  } finally {
    // Ensure cleanup happens regardless of success/failure
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
  }
};

/**
 * API 호출 전 서버 상태 확인 및 안전한 요청 함수
 * @param url 요청 URL
 * @param options 요청 옵션
 * @param skipHealthCheck 헬스체크를 건너뛸지 여부 (성능 최적화용)
 * @returns Promise<ApiResponse>
 */
export const safeApiCall = async (
  url: string,
  options: RequestInit = {},
  skipHealthCheck: boolean = false
): Promise<ApiResponse> => {
  try {
    // 옵션에 따라 서버 상태 확인 건너뛰기 가능
    if (!skipHealthCheck) {
      const isServerHealthy = await checkServerHealth();
      if (!isServerHealthy) {
        return {
          success: false,
          error: '서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.',
          errorType: ApiErrorType.SERVER_UNREACHABLE
        };
      }
    }

    // 실제 API 호출
    const response = await fetch(url, options);

    // Content-Type 확인
    const contentType = response.headers.get('content-type');
    const isJson = contentType?.includes('application/json') ?? false;

    if (!isJson) {
      const responseText = await response.text();
      console.error('Non-JSON response received:', {
        url,
        status: response.status,
        contentType,
        responseText: responseText.substring(0, RESPONSE_TEXT_PREVIEW_LENGTH)
      });

      return {
        success: false,
        error: '서버 응답 형식이 올바르지 않습니다. 서버 상태를 확인해주세요.',
        errorType: ApiErrorType.INVALID_RESPONSE_FORMAT
      };
    }

    const data = await response.json();

    if (!response.ok) {
      return {
        success: false,
        error: data.message || `서버 오류 (${response.status})`,
        errorType: ApiErrorType.SERVER_ERROR
      };
    }

    return {
      success: true,
      data
    };

  } catch (error: any) {
    console.error('API 호출 중 오류:', error);

    // More specific error handling
    if (error.name === 'AbortError') {
      return {
        success: false,
        error: '요청 시간이 초과되었습니다. 네트워크 연결을 확인해주세요.',
        errorType: ApiErrorType.TIMEOUT
      };
    }

    if (error.message?.includes('Network request failed')) {
      return {
        success: false,
        error: '네트워크 연결을 확인해주세요.',
        errorType: ApiErrorType.NETWORK_ERROR
      };
    }

    if (error.message?.includes('JSON Parse error')) {
      return {
        success: false,
        error: '서버가 예상과 다른 응답을 보냈습니다. 서버 상태를 확인해주세요.',
        errorType: ApiErrorType.JSON_PARSE_ERROR
      };
    }

    return {
      success: false,
      error: '요청 처리 중 오류가 발생했습니다.',
      errorType: ApiErrorType.UNKNOWN_ERROR
    };
  }
};
