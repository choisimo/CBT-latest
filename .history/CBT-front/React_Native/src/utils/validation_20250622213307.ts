// src/utils/validation.ts

/**
 * 식별자 유효성 검증 (이메일 또는 로그인 ID)
 * @param identifier 검증할 식별자
 * @returns 유효한 식별자인지 여부
 */
export const isValidIdentifier = (identifier: string): boolean => {
  if (!identifier || identifier.trim() === '') {
    return false;
  }

  // 유효하지 않은 값들 체크
  const invalidValues = [
    'NONE_PROVIDED',
    'null',
    'undefined',
    'none',
    'empty',
    ''
  ];

  const normalizedIdentifier = identifier.trim().toLowerCase();
  
  if (invalidValues.includes(normalizedIdentifier)) {
    return false;
  }

  return true;
};

/**
 * 이메일 형식 검증
 * @param email 검증할 이메일
 * @returns 유효한 이메일 형식인지 여부
 */
export const isValidEmail = (email: string): boolean => {
  if (!isValidIdentifier(email)) {
    return false;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * 로그인 ID 형식 검증
 * @param loginId 검증할 로그인 ID
 * @returns 유효한 로그인 ID인지 여부
 */
export const isValidLoginId = (loginId: string): boolean => {
  if (!isValidIdentifier(loginId)) {
    return false;
  }

  // 로그인 ID는 최소 4자 이상
  return loginId.trim().length >= 4;
};

/**
 * 비밀번호 형식 검증
 * @param password 검증할 비밀번호
 * @returns 유효한 비밀번호인지 여부
 */
export const isValidPassword = (password: string): boolean => {
  if (!password || password.trim() === '') {
    return false;
  }

  // 비밀번호는 최소 8자 이상
  return password.length >= 8;
};

/**
 * 로그인 요청 데이터 생성
 * @param identifier 이메일 또는 로그인 ID
 * @param password 비밀번호
 * @returns 서버로 전송할 로그인 요청 데이터
 */
export const createLoginPayload = (identifier: string, password: string) => {
  if (!isValidIdentifier(identifier) || !isValidPassword(password)) {
    throw new Error('유효하지 않은 로그인 정보입니다');
  }

  const isEmail = identifier.includes('@');
  
  if (isEmail) {
    if (!isValidEmail(identifier)) {
      throw new Error('올바른 이메일 형식을 입력하세요');
    }
    return { email: identifier, password };
  } else {
    if (!isValidLoginId(identifier)) {
      throw new Error('아이디는 최소 4자 이상이어야 합니다');
    }
    return { loginId: identifier, password };
  }
};

/**
 * 로그인 에러 메시지 정제
 * @param error 원본 에러 객체 또는 메시지
 * @returns 사용자 친화적인 에러 메시지
 */
export const getLoginErrorMessage = (error: any): string => {
  if (typeof error === 'string') {
    return error;
  }

  if (error?.message) {
    // 서버에서 오는 특정 에러 메시지들 처리
    const message = error.message.toLowerCase();
    
    if (message.includes('invalid credentials') || message.includes('bad credentials')) {
      return '아이디 또는 비밀번호가 올바르지 않습니다';
    }
    
    if (message.includes('user not found')) {
      return '존재하지 않는 사용자입니다';
    }
    
    if (message.includes('none_provided')) {
      return '이메일 또는 아이디를 입력해주세요';
    }
    
    return error.message;
  }

  return '로그인 중 오류가 발생했습니다';
};
