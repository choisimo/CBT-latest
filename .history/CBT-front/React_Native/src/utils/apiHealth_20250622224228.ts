// src/utils/apiHealth.ts
import { BASIC_URL } from '../constants/api';

/**
 * 서버 상태 확인 함수
 * @returns Promise<boolean> 서버가 정상적으로 응답하는지 여부
 */
export const checkServerHealth = async (): Promise<boolean> => {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5초 타임아웃
    
    const response = await fetch(`${BASIC_URL}/actuator/health`, {
      method: 'GET',
      signal: controller.signal,
    });
    
    clearTimeout(timeoutId);
    return response.ok;
  } catch (error) {
    console.warn('서버 상태 확인 실패:', error);
    return false;
  }
};

/**
 * API 호출 전 서버 상태 확인 및 안전한 요청 함수
 * @param url 요청 URL
 * @param options 요청 옵션
 * @returns Promise<{success: boolean, data?: any, error?: string}>
 */
export const safeApiCall = async (
  url: string, 
  options: RequestInit = {}
): Promise<{success: boolean, data?: any, error?: string}> => {
  try {
    // 먼저 서버 상태 확인
    const isServerHealthy = await checkServerHealth();
    if (!isServerHealthy) {
      return {
        success: false,
        error: '서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.'
      };
    }

    // 실제 API 호출
    const response = await fetch(url, options);
    
    // Content-Type 확인
    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.indexOf('application/json') !== -1;
    
    if (!isJson) {
      const responseText = await response.text();
      console.error('Non-JSON response received:', {
        url,
        status: response.status,
        contentType,
        responseText: responseText.substring(0, 500)
      });
      
      return {
        success: false,
        error: '서버 응답 형식이 올바르지 않습니다. 서버 상태를 확인해주세요.'
      };
    }

    const data = await response.json();
    
    if (!response.ok) {
      return {
        success: false,
        error: data.message || `서버 오류 (${response.status})`
      };
    }

    return {
      success: true,
      data
    };
    
  } catch (error: any) {
    console.error('API 호출 중 오류:', error);
    
    if (error.name === 'AbortError') {
      return {
        success: false,
        error: '요청 시간이 초과되었습니다. 네트워크 연결을 확인해주세요.'
      };
    }
    
    if (error.message && error.message.includes('Network request failed')) {
      return {
        success: false,
        error: '네트워크 연결을 확인해주세요.'
      };
    }
    
    if (error.message && error.message.includes('JSON Parse error')) {
      return {
        success: false,
        error: '서버가 예상과 다른 응답을 보냈습니다. 서버 상태를 확인해주세요.'
      };
    }
    
    return {
      success: false,
      error: '요청 처리 중 오류가 발생했습니다.'
    };
  }
};
