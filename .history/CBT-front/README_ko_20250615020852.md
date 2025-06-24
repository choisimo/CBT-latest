# 📱 CBT-Diary 프론트엔드

CBT-Diary 프론트엔드 애플리케이션에 오신 것을 환영합니다. 이는 감정 분석과 인지행동치료(CBT) 기반의 일기 작성을 도와주는 React Native 애플리케이션입니다.

## 🚀 빠른 시작

### 📋 사전 요구사항

```mermaid
graph TB
    subgraph "개발 환경"
        A[Node.js 18+]
        B[React Native CLI]
        C[Android Studio]
        D[Xcode - iOS용]
        E[JDK 11+]
    end
    
    A --> B
    B --> C
    B --> D
    C --> E
    
    style A fill=#68c944,color:#fff
    style B fill=#61dafb,color:#000
    style C fill:#3ddc84,color:#fff
    style D fill=#007acc,color:#fff
    style E fill=#ed8b00,color:#fff
```

- **Node.js** v18 이상
- **React Native CLI** 최신 버전
- **개발 도구**:
  - Android 개발: Android Studio + JDK 11+
  - iOS 개발: Xcode (macOS만 해당)

### 🔧 설치 및 실행

#### 1️⃣ 의존성 설치
```bash
# npm 사용
npm install

# 또는 yarn 사용
yarn install
```

#### 2️⃣ 개발 서버 시작
```bash
npx react-native start
```

#### 3️⃣ 앱 실행

**Android:**
```bash
npx react-native run-android
```

**iOS:**
```bash
npx react-native run-ios
```

## 📁 프로젝트 구조

```mermaid
graph TB
    subgraph "📂 프로젝트 구조"
        A[src/]
        B[├── auth/]
        C[├── constants/]
        D[├── context/]
        E[├── navigation/]
        F[└── screens/]
        
        B1[└── oauthConfig.ts]
        C1[└── api.ts]
        D1[└── AuthContext.tsx]
        E1[├── AppStack.tsx]
        E2[├── AuthStack.tsx]
        E3[└── RootNavigator.tsx]
        F1[├── auth/]
        F2[└── main/]
    end
    
    A --> B
    A --> C
    A --> D
    A --> E
    A --> F
    
    B --> B1
    C --> C1
    D --> D1
    E --> E1
    E --> E2
    E --> E3
    F --> F1
    F --> F2
    
    style A fill:#2196f3,color:#fff
    style B fill:#4caf50,color:#fff
    style C fill:#ff9800,color:#fff
    style D fill:#9c27b0,color:#fff
    style E fill=#f44336,color:#fff
    style F fill=#00bcd4,color:#fff
```

### 📂 디렉토리 설명

| 폴더 | 설명 |
|------|------|
| 🔐 `src/auth/` | OAuth 설정 및 인증 관련 유틸리티 |
| 🔧 `src/constants/` | API 엔드포인트 및 상수 정의 |
| 🔄 `src/context/` | React Context 및 전역 상태 관리 |
| 🧭 `src/navigation/` | 화면 간 네비게이션 설정 |
| 📱 `src/screens/` | 화면 컴포넌트들 |

## ✨ 주요 기능

```mermaid
graph LR
    subgraph "🔐 인증"
        A[OAuth 로그인]
        B[회원가입]
        C[로그아웃]
    end
    
    subgraph "📚 일기"
        D[일기 작성]
        E[일기 목록]
        F[일기 수정/삭제]
    end
    
    subgraph "🧠 AI 분석"
        G[감정 분석]
        H[CBT 추천]
        I[통계 제공]
    end
    
    A --> D
    B --> D
    D --> G
    E --> F
    G --> H
    H --> I
    
    style A fill:#e3f2fd
    style B fill=#f3e5f5
    style C fill=#fff3e0
    style D fill=#e8f5e8
    style E fill=#fce4ec
    style F fill=#f1f8e9
    style G fill=#fff8e1
    style H fill=#ffebee
    style I fill=#f3e5f5
```

### 🎯 핵심 기능
- **🔐 사용자 인증**: OAuth를 통한 안전한 로그인/회원가입
- **📚 일기 관리**: 일기 작성, 수정, 삭제 및 목록 보기
- **🧠 AI 감정 분석**: 작성된 일기의 감정 상태 분석
- **💡 CBT 추천**: 인지행동치료 기반 개선 제안
- **📊 통계 및 리포트**: 감정 변화 추적 및 시각화

## 🔧 개발 도구

### 📦 주요 라이브러리

```json
{
  "react": "^18.2.0",
  "react-native": "^0.72.0",
  "@react-navigation/native": "^6.0.0",
  "@react-navigation/stack": "^6.0.0",
  "react-native-config": "^1.5.0"
}
```

### 🛠️ 빌드 및 테스트

```bash
# 테스트 실행
npm test

# 타입 체크 (TypeScript)
npx tsc --noEmit

# Android Release 빌드
cd android && ./gradlew assembleRelease
```

## 🌐 환경 설정

### 📋 필요한 환경 변수

`.env` 파일을 생성하여 다음 변수들을 설정하세요:

```env
# API 설정
API_BASE_URL=https://your-api-server.com
API_TIMEOUT=10000

# OAuth 설정
NAVER_CLIENT_ID=your_naver_client_id
KAKAO_CLIENT_ID=your_kakao_client_id

# 기타 설정
APP_ENV=development
DEBUG_MODE=true
```

## 🚨 문제 해결

### 일반적인 문제들

| 문제 | 해결 방법 |
|------|----------|
| 🔴 Metro 서버 시작 실패 | `npx react-native start --reset-cache` |
| 🔴 Android 빌드 실패 | `cd android && ./gradlew clean` |
| 🔴 iOS 빌드 실패 | Xcode에서 클린 빌드 수행 |
| 🔴 의존성 문제 | `rm -rf node_modules && npm install` |

### 📞 지원

문제가 발생하거나 기능 요청이 있으시면:

1. 📝 [이슈 트래커](../../issues)에서 기존 이슈 확인
2. 🆕 새로운 이슈 생성 (템플릿 사용)
3. 📧 개발팀에 직접 연락

## 🤝 기여하기

프로젝트에 기여하고 싶으시다면:

1. 🍴 저장소 포크
2. 🌟 새로운 기능 브랜치 생성 (`feature/amazing-feature`)
3. 💾 변경사항 커밋
4. 📤 브랜치에 푸시
5. 🔀 Pull Request 생성

---

> 💡 **팁**: 개발하면서 문제가 생기면 [문서](../../docs/front/)를 먼저 확인해보세요!
