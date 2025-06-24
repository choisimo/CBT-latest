// src/constants/api.ts

// 개발 환경 감지
const isDevelopment = __DEV__;

import { Platform } from 'react-native';
// Android 에뮬레이터에서는 PC 호스트 접근을 위해 10.0.2.2 사용
const LOCALHOST = '10.0.2.2';

// 로컬 개발용 URL (Docker 컨테이너 포트 7078)
const LOCAL_URL = `http://${LOCALHOST}:7078`;

// 프로덕션 URL
const PRODUCTION_URL = 'https://auth.nodove.com';

// 환경에 따른 API URL 선택
// 임시로 로컬 테스트를 위해 로컬 URL 사용
export const BASIC_URL = LOCAL_URL; // isDevelopment ? LOCAL_URL : PRODUCTION_URL;
