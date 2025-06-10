## 📄 Code Scan Report: Auth & CBT Modules

**분석 일자:** 2024-05-16
**리포지토리 (백엔드):** `Auth-server`
**리포지토리 (프론트엔드):** `CBT-front`

## 🚀 요약

이번 코드 스캔을 통해 발견된 주요 사항 요약.
- **Critical (API 불일치)**: 12 건
- **Major (미구현 기능)**: 10 건
- **Minor (개선 제안)**: 4 건

---

### 📝 목차

1.  [🚀 요약](#-요약)
2.  [🚨 Critical: API 불일치 (REQ-SCAN-02)](#1-critical-api-불일치-req-scan-02)
3.  [🚧 Major: 미구현 기능 (REQ-SCAN-03)](#2-major-미구현-기능-req-scan-03)
4.  [💡 Minor: 코드 개선 제안 (REQ-SCAN-04)](#3-minor-코드-개선-제안-req-scan-04)

---

### 1. 🚨 Critical: API 불일치 (REQ-SCAN-02)

API 문서와 프론트엔드 코드 간의 불일치 목록입니다.

#### 일반적인 URL 구성 오류

*   **위치:** `CBT-front/src/context/AuthContext.tsx`, `CBT-front/src/screens/auth/EmailVerificationScreen.tsx`, `CBT-front/src/screens/main/WriteScreen.tsx`, `CBT-front/src/screens/main/ViewScreen.tsx`, `CBT-front/src/screens/main/AnalyzeScreen.tsx`, `CBT-front/src/screens/main/MainScreen.tsx`
*   **프론트엔드 기대값:** API 요청 시 `https://${BASIC_URL}/<path>` 형식으로 URL 구성.
*   **백엔드 명세 (`CBT-front/src/constants/api.ts`):** `BASIC_URL`이 이미 `https://your-api-domain.com`으로 `https://`를 포함하고 있음.
*   **문제점:** URL이 `https://https://your-api-domain.com/...` 와 같이 중복된 스키마로 잘못 구성됩니다.
*   **권장 조치:** `BASIC_URL` 사용 시 `https://` 접두사를 중복으로 사용하지 않도록 프론트엔드 코드 수정. 예를 들어, `fetch(\`\${BASIC_URL}/api/auth/login\`, ...)`과 같이 사용해야 합니다. `SignupScreen.tsx`는 올바르게 사용 중입니다.

---

#### 1.1. 인증 관련 API

##### 로그인 응답에 사용자 정보 누락

*   **위치:** `CBT-front/src/context/AuthContext.tsx` (signIn 함수)
*   **프론트엔드 기대값:** 로그인 성공 시 응답 본문에 `{ access_token, user }` 객체를 기대합니다.
*   **백엔드 명세 (5.1 로그인):** 응답 본문으로 `{ "access_token": "..." }` 만 명시되어 있고, `user` 객체는 포함되지 않습니다.
*   **권장 조치:** 백엔드 로그인 API가 응답에 `user` 객체를 포함하도록 수정하거나, 프론트엔드가 로그인 후 별도로 사용자 정보를 조회하도록 변경합니다. (현재 `bootstrapAsync`에서 `/api/users/me` 호출로 해결하려는 시도가 보이나 해당 엔드포인트도 미정의 상태)

##### 로그아웃 엔드포인트 미정의

*   **위치:** `CBT-front/src/context/AuthContext.tsx` (signOut 함수)
*   **프론트엔드 기대값:** `POST` `https://${BASIC_URL}/api/auth/logout` 경로로 로그아웃 요청.
*   **백엔드 명세:** `/api/auth/logout` 엔드포인트가 API 문서에 정의되어 있지 않습니다. 문서에는 `POST /api/public/clean/userTokenCookie` (4.6)가 존재하나, 이는 쿠키의 리프레시 토큰 제거용으로 목적이 다를 수 있습니다.
*   **권장 조치:** 백엔드에 `/api/auth/logout` 엔드포인트를 명세에 맞게 추가하거나, 프론트엔드가 문서화된 로그아웃 관련 엔드포인트(예: `4.6. 사용자 토큰 쿠키 정리`)를 사용하도록 수정합니다.

##### 사용자 정보 조회 엔드포인트 미정의

*   **위치:** `CBT-front/src/context/AuthContext.tsx` (bootstrapAsync, refreshUser 함수)
*   **프론트엔드 기대값:** `GET` `https://${BASIC_URL}/api/users/me` 경로로 현재 사용자 정보 요청.
*   **백엔드 명세:** `/api/users/me` 엔드포인트가 API 문서에 정의되어 있지 않습니다. (사용자 관리 API는 섹션 4에 있지만 해당 경로는 없음)
*   **권장 조치:** 백엔드에 `/api/users/me` 엔드포인트를 명세에 맞게 추가하고, 응답 데이터 구조(프론트엔드는 `{ user }` 기대)를 명확히 합니다.

---

#### 1.2. 회원가입 및 이메일 인증 API

##### 회원가입 요청 필드 불일치 및 이메일 인증 연동 누락

*   **위치:** `CBT-front/src/screens/auth/SignupScreen.tsx`
*   **프론트엔드 기대값 (요청):** `POST` `${BASIC_URL}/api/public/join` 경로로 `{ email, userPw, userName, nickname }` 전송.
*   **백엔드 명세 (4.1 회원 가입):**
    *   요청 본문: `{ "userId": "...", "userPw": "...", "userName": "...", "nickname": "...", "phone": "...", "email": "...", "role": "USER", "birthDate": "...", "gender": "...", "isPrivate": false, "profile": "...", "code": "..." }` 를 기대합니다.
    *   `userId` 필드가 필수적이나 프론트엔드에서 전송하지 않습니다 (대신 `email`을 ID처럼 사용하려는 의도로 보임).
    *   `code` (이메일 인증 코드) 필드가 필요하다고 설명되어 있으나, 프론트엔드 회원가입 요청에는 포함되지 않습니다.
*   **권장 조치:**
    *   프론트엔드에서 `userId` 필드를 사용자로부터 입력받거나, `email`을 `userId`로 사용한다면 백엔드와 협의하여 명세 일치.
    *   회원가입 시 이메일 인증 코드(`code`)를 요청 본문에 포함하도록 수정합니다. 이를 위해 이메일 인증 절차(1.1, 1.3)가 회원가입 전에 완료되고 그 결과(인증 코드)가 전달되어야 합니다.

##### 이메일 인증 확인 요청 경로 및 필드 불일치

*   **위치:** `CBT-front/src/screens/auth/EmailVerificationScreen.tsx`
*   **프론트엔드 기대값 (요청):** `POST` `https://${BASIC_URL}/api/auth/verify-email` 경로로 `{ code }` 전송.
*   **백엔드 명세 (1.3 이메일 인증 코드 확인):**
    *   엔드포인트: `/api/public/emailCheck`
    *   요청 본문: `{ "email": "user@example.com", "code": "A1B2C3D4" }`
*   **권장 조치:**
    *   프론트엔드가 사용하는 엔드포인트 경로를 `/api/public/emailCheck`로 수정합니다.
    *   요청 본문에 `email` 필드를 추가합니다. 이메일 값을 이전 화면(아마도 이메일 입력 화면)에서 받아와 함께 전송해야 합니다.

---

#### 1.3. 다이어리 (일기) API

##### 다이어리 로드 (특정 글 상세 조회) 경로 및 응답 데이터 불일치

*   **위치:** `CBT-front/src/screens/main/WriteScreen.tsx` (loadExistingPost), `CBT-front/src/screens/main/ViewScreen.tsx` (useEffect loadPost)
*   **프론트엔드 기대값:**
    *   경로: `GET` `https://${BASIC_URL}/api/diaryposts/${postId}`
    *   응답: `{ id: string, date: 'YYYY-MM-DD', title: string, content: string, aiResponse: boolean }`
*   **백엔드 명세 (6.3 특정 일기 상세 조회):**
    *   경로: `GET` `/api/diaries/{diaryId}`
    *   응답: `{ id, userId, title, content, alternativeThoughtByAI, createdAt, updatedAt, analysis: { ... } }`
*   **권장 조치:**
    *   프론트엔드가 사용하는 엔드포인트 경로를 `/api/diaries/${postId}`로 수정합니다.
    *   프론트엔드가 응답 데이터 구조 차이를 처리하도록 수정:
        *   `date` 필드 대신 백엔드의 `createdAt` (타임스탬프)을 사용하고 'YYYY-MM-DD' 형식으로 변환합니다.
        *   `aiResponse` boolean 필드 대신 백엔드의 `analysis` 객체 유무 또는 `analysis.status` 값, 혹은 `alternativeThoughtByAI` 필드의 존재 유무로 AI 분석 여부를 판단하도록 로직을 수정합니다.

##### 다이어리 수정 경로 불일치

*   **위치:** `CBT-front/src/screens/main/WriteScreen.tsx` (handleSubmit - 수정 모드)
*   **프론트엔드 기대값 (요청):** `PUT` `https://${BASIC_URL}/api/diaryposts/${postId}` 경로로 `{ title, content }` 전송.
*   **백엔드 명세 (6.4 특정 일기 수정):**
    *   경로: `PUT` `/api/diaries/{diaryId}`
    *   요청 본문: `{ "title": "...", "content": "..." }` (요청 본문 구조는 일치)
*   **권장 조치:** 프론트엔드가 사용하는 엔드포인트 경로를 `/api/diaries/${postId}`로 수정합니다.

##### 다이어리 생성 경로, 요청 및 응답 데이터 불일치

*   **위치:** `CBT-front/src/screens/main/WriteScreen.tsx` (handleSubmit - 새 글 모드)
*   **프론트엔드 기대값:**
    *   경로: `POST` `https://${BASIC_URL}/api/diaryposts`
    *   요청: `{ date: 'YYYY-MM-DD', title: string, content: string }`
    *   응답: `{ postId: newPostId }`
*   **백엔드 명세 (6.1 일기 작성):**
    *   경로: `POST` `/api/diaries`
    *   요청: `{ "title": "...", "content": "..." }` (백엔드는 `date` 필드를 받지 않음)
    *   응답: `{ id, userId, title, content, createdAt, updatedAt }` (전체 일기 객체)
*   **권장 조치:**
    *   프론트엔드가 사용하는 엔드포인트 경로를 `/api/diaries`로 수정합니다.
    *   프론트엔드 요청 시 `date` 필드를 제거합니다 (날짜는 서버에서 `createdAt`으로 자동 생성됨).
    *   프론트엔드가 백엔드 응답에서 `id` 값을 `postId`로 사용하도록 수정합니다.

##### AI 분석 요청 엔드포인트 미정의 또는 불일치

*   **위치:** `CBT-front/src/screens/main/ViewScreen.tsx` (handleAnalyze 함수)
*   **프론트엔드 기대값:** `POST` `https://${BASIC_URL}/api/diaries/${post.id}/analysis` 경로로 AI 분석 요청 (결과가 없을 시).
*   **백엔드 명세:**
    *   문서에는 `POST` `/api/diaries/{diaryId}/analysis`와 같이 분석을 *요청/트리거*하는 엔드포인트가 명시되어 있지 않습니다.
    *   일기 상세 조회(6.3) 시 `analysis` 객체가 포함되어 반환됩니다.
*   **권장 조치:**
    *   백엔드에 AI 분석을 명시적으로 요청하는 `POST /api/diaries/{diaryId}/analysis` 엔드포인트가 실제로 존재하는지 확인하고, 존재한다면 문서에 추가합니다.
    *   만약 해당 엔드포인트가 없다면, 프론트엔드는 AI 분석 상태를 확인하기 위해 기존 일기 상세 조회(6.3)를 다시 호출하거나, 다른 방식으로 분석 완료를 확인해야 합니다. (현재 `AnalyzeScreen.tsx`는 `GET`으로 해당 경로를 호출하여 분석 결과를 가져오려 함 - 아래 항목 참고)

##### AI 분석 결과 조회 경로 및 응답 데이터 구조 불일치

*   **위치:** `CBT-front/src/screens/main/AnalyzeScreen.tsx` (loadAnalysis 함수)
*   **프론트엔드 기대값:**
    *   경로: `GET` `https://${BASIC_URL}/api/diaries/${postId}/analysis`
    *   응답 (진행 중): `{ message, progress, estimatedRemaining }`
    *   응답 (완료): `{ analysis: AnalysisResult }` 또는 직접 `AnalysisResult` (코드와 인터페이스 간 약간의 모호성 존재)
    *   `AnalysisResult.emotionDetection`: `{ joy, sadness, surprise, calm }` (객체 형태)
    *   `AnalysisResult`에 `emotionSummary`, `confidence` 필드 기대.
*   **백엔드 명세 (6.3 특정 일기 상세 조회):**
    *   `/api/diaries/{diaryId}/analysis` 라는 별도 GET 경로는 문서화되어 있지 않음. 분석 결과는 `/api/diaries/{diaryId}` 응답의 `analysis` 객체에 포함.
    *   `analysis.emotionDetection`: `"기쁨 80%, 슬픔 10%, 놀람 5%, 평온 5%"` (문자열 형태)
    *   `analysis` 객체에 `emotionSummary`, `confidence` 필드 없음.
    *   분석 진행 상태(message, progress 등)를 반환하는 API 명세 없음.
*   **권장 조치:**
    *   프론트엔드는 AI 분석 결과를 얻기 위해 `/api/diaries/${postId}`를 호출하고, 응답에서 `analysis` 객체를 사용하도록 수정합니다. 별도의 `/analysis` GET 경로는 불필요해 보입니다.
    *   `emotionDetection` 필드의 구조 불일치를 해결합니다. 프론트엔드가 백엔드의 문자열 형식을 파싱하여 객체로 만들거나, 백엔드에서 응답 형식을 객체로 변경해야 합니다.
    *   `emotionSummary`, `confidence` 필드는 백엔드 응답에 없으므로, 프론트엔드에서 제거하거나 백엔드에 추가 요청합니다.
    *   분석 진행 상태 폴링 API가 필요하다면 백엔드와 협의하여 새 API를 정의하고 문서화해야 합니다.

##### 다이어리 목록 조회 쿼리 파라미터 및 응답 데이터 불일치

*   **위치:** `CBT-front/src/screens/main/MainScreen.tsx` (loadDates, loadAllOrSearch, loadByDate 함수)
*   **프론트엔드 기대값:**
    *   경로: `GET` `https://${BASIC_URL}/api/diaries` + query string
    *   쿼리 파라미터: `searchText`, `startDate`, `endDate` 사용.
    *   응답: `{ dates: string[], diaries: [{ id, title, date }], totalCount: number }`
*   **백엔드 명세 (6.2 일기 목록 조회):**
    *   경로: `GET` `/api/diaries` (일치)
    *   쿼리 파라미터: `page`, `size`, `sort` 만 명시됨. (`searchText`, `startDate`, `endDate`는 미언급)
    *   응답: `{ diaries: [{ id, title, createdAt, emotionStatus }], pageInfo: { currentPage, totalPages, totalElements } }`
*   **권장 조치:**
    *   백엔드에서 `searchText`, `startDate`, `endDate` 쿼리 파라미터를 지원하는지 확인하고, 지원한다면 문서에 명시합니다. 아니라면 프론트엔드에서 해당 파라미터 사용을 재검토합니다.
    *   프론트엔드가 응답 데이터 구조 차이를 처리하도록 수정:
        *   `dates: string[]` 필드는 백엔드에 없으므로, 프론트엔드에서 `diaries` 목록의 `createdAt` 값을 가공하여 `allDates` 상태를 만들어야 합니다 (현재 `loadDates` 함수가 이를 시도하는 것으로 보이나, 응답에 `dates`가 있다고 가정하고 있음).
        *   `diaries` 배열의 아이템에서 `date` 필드 대신 `createdAt`을 사용하고 형식 변환.
        *   `totalCount` 대신 `pageInfo.totalElements`를 사용.

---

### 2. 🚧 Major: 미구현 기능 (REQ-SCAN-03)

프론트엔드에서 아직 구현되지 않은 백엔드 API 기능 목록입니다.

#### 2.1. 사용자 인증 및 관리

##### 임시 비밀번호 발송 (로그인 사용자)
*   **기능 이름:** 로그인된 사용자를 위한 임시 비밀번호 발송 기능
*   **관련 API:** `POST /api/protected/sendEmailPassword`
*   **상세:** 사용자가 로그인 상태에서 자신의 이메일로 임시 비밀번호를 받을 수 있는 기능입니다. 프로필 설정이나 계정 관리 화면에 포함될 수 있습니다.
*   **구현 우선순위:** 중간

##### 이메일로 임시 비밀번호 찾기 (공개)
*   **기능 이름:** 아이디를 이용한 임시 비밀번호 찾기 기능
*   **관련 API:** `GET /api/public/findPassWithEmail`
*   **상세:** 사용자가 아이디를 입력하여 등록된 이메일로 임시 비밀번호를 받을 수 있는 기능입니다. 로그인 화면의 "비밀번호 찾기" 등에 활용될 수 있습니다.
*   **구현 우선순위:** 높음 (REQ-SCAN-03에서 비밀번호 찾기/재설정 기능 누락 언급)

##### 프로필 이미지 업로드
*   **기능 이름:** 사용자 프로필 이미지 업로드 기능
*   **관련 API:** `POST /api/public/profileUpload`
*   **상세:** 사용자가 자신의 프로필 이미지를 업로드하고 변경할 수 있는 기능입니다. 사용자 프로필 수정 화면에서 필요합니다.
*   **구현 우선순위:** 중간

##### 사용자 ID 중복 체크
*   **기능 이름:** 회원가입 시 사용자 ID 중복 체크 기능
*   **관련 API:** `POST /api/public/check/userId/IsDuplicate`
*   **상세:** 회원가입 과정에서 사용자가 입력한 ID의 중복 여부를 실시간으로 확인할 수 있는 기능입니다. `SignupScreen.tsx`에 연동 필요.
*   **구현 우선순위:** 높음 (REQ-SCAN-03에서 회원가입 시 중복검사 누락 언급)

##### 닉네임 중복 체크
*   **기능 이름:** 회원가입 또는 프로필 수정 시 닉네임 중복 체크 기능
*   **관련 API:** `POST /api/public/check/nickname/IsDuplicate`
*   **상세:** 회원가입이나 프로필 수정 시 사용자가 입력한 닉네임의 중복 여부를 실시간으로 확인할 수 있는 기능입니다. `SignupScreen.tsx` 및 프로필 수정 화면에 연동 필요.
*   **구현 우선순위:** 높음 (REQ-SCAN-03에서 회원가입 시 중복검사 누락 언급)

##### OAuth2 소셜 로그인 전체 기능
*   **기능 이름:** OAuth2 소셜 로그인 (Google, Kakao, Naver)
*   **관련 API:**
    *   `GET /api/public/oauth2/login_url/{provider}`
    *   `GET /api/public/oauth2/callback/{provider}`
    *   `POST /oauth2/callback/kakao`
    *   `POST /oauth2/callback/naver`
    *   `POST /oauth2/callback/google`
*   **상세:** 사용자가 Google, Kakao, Naver 계정을 통해 서비스에 로그인하거나 가입할 수 있도록 하는 전체적인 OAuth2 연동 기능입니다. 로그인 화면 등에 소셜 로그인 버튼 추가 및 콜백 처리 로직 구현이 필요합니다.
*   **구현 우선순위:** 높음 (REQ-SCAN-03에서 소셜 로그인 기능 누락 언급)

##### 사용자 토큰 쿠키 정리 (소프트 로그아웃 대안)
*   **기능 이름:** 클라이언트 refreshToken 쿠키 제거
*   **관련 API:** `POST /api/public/clean/userTokenCookie`
*   **상세:** 현재 프론트엔드는 `/api/auth/logout` (미정의 엔드포인트)을 호출하여 로그아웃을 시도합니다. 만약 완전한 서버 세션 로그아웃이 아니라 클라이언트 측 토큰 정리만을 원한다면 이 API를 사용할 수 있습니다. 다만, 현재 `AuthContext`의 `signOut`은 이미 `Keychain.resetGenericPassword()`로 로컬 토큰을 제거하고 있어, 이 API의 필요성은 백엔드 로그아웃 전략에 따라 결정됩니다.
*   **구현 우선순위:** 낮음 (현재 프론트엔드 로그아웃 방식 고려 시)

---

#### 2.2. 다이어리 (일기) 관련 기능

##### 일기 삭제 기능
*   **기능 이름:** 일기 삭제 기능
*   **관련 API:** `DELETE /api/diaries/{diaryId}`
*   **상세:** 사용자가 작성한 일기를 삭제할 수 있는 UI(예: ViewScreen에 삭제 버튼 추가) 및 API 연동 로직이 필요합니다.
*   **구현 우선순위:** 높음 (REQ-SCAN-03에서 CRUD 중 Delete 누락 언급)

---

#### 2.3. 사용자 설정

##### 사용자 설정 조회
*   **기능 이름:** 사용자 설정 조회 기능
*   **관련 API:** `GET /api/settings`
*   **상세:** 사용자의 개인화된 설정을 불러와 화면에 표시하는 기능입니다. (예: 알림 설정, 테마 설정 등)
*   **구현 우선순위:** 중간

##### 사용자 설정 수정
*   **기능 이름:** 사용자 설정 수정 기능
*   **관련 API:** `PUT /api/settings`
*   **상세:** 사용자가 자신의 설정을 변경하고 저장할 수 있는 기능입니다.
*   **구현 우선순위:** 중간

---

### 3. 💡 Minor: 코드 개선 제안 (REQ-SCAN-04)

코드 품질 및 일관성 향상을 위한 제안 사항입니다.

#### 3.1. 오류 처리

##### 일반 오류 메시지 구체화
*   **위치:** `CBT-front/src/screens/auth/SignupScreen.tsx` (handleSignup), `CBT-front/src/screens/main/WriteScreen.tsx` (handleSubmit)
*   **내용:** 현재 일부 `catch` 블록에서 일반적인 '네트워크 오류가 발생했습니다' 메시지를 사용합니다. 이는 실제 오류 원인이 네트워크가 아닐 경우 사용자에게 혼동을 줄 수 있습니다. 예를 들어, `res.json()` 파싱 실패나 기타 클라이언트 측 로직 오류일 수도 있습니다.
*   **제안:** 오류 객체(e.g., `error.message`)를 분석하여 더 구체적인 오류 메시지를 제공하거나, 최소한 '알 수 없는 오류가 발생했습니다.'와 같이 보다 포괄적인 메시지로 변경하는 것을 고려합니다.

##### AuthContext의 부트스트랩 오류 알림 부재
*   **위치:** `CBT-front/src/context/AuthContext.tsx` (bootstrapAsync 함수)
*   **내용:** `bootstrapAsync` 내 `catch` 블록에서 발생하는 오류는 `console.warn`으로만 기록되고 사용자에게 별도의 알림이 없습니다. 만약 토큰 로드나 사용자 정보 초기 로드에 실패하면 앱이 비정상적으로 동작할 수 있습니다.
*   **제안:** 치명적인 초기화 실패 시 (예: 저장된 토큰은 있으나 사용자 정보 로드 실패) 사용자에게 "앱 초기화 중 문제가 발생했습니다. 앱을 재시작하거나 지원팀에 문의하세요." 와 같은 알림을 제공하는 것을 고려합니다.

#### 3.2. 상태 관리

*   **전반적인 상태 관리:** 현재 분석된 파일들에서는 `AuthContext`를 통한 전역 인증 상태 관리와 `useState`를 통한 화면별 로컬 상태 관리가 적절히 분리되어 사용되고 있는 것으로 보입니다. 특별한 오용 사례는 발견되지 않았습니다.

#### 3.3. 주석 처리된 코드

##### RootNavigator의 스택 분기 로직
*   **위치:** `CBT-front/src/navigation/RootNavigator.tsx` (라인 20 근처)
*   **내용:** `// {!user || !user.emailVerified ? <AuthStack /> : <AppStack />}` 라인이 주석 처리되어 있고, 현재는 `{<AppStack />}`만 사용되어 인증 상태나 이메일 인증 여부에 관계없이 `AppStack`으로 진입하게 됩니다.
*   **제안:** 해당 주석은 의도된 동작인지 확인이 필요합니다. 만약 인증 여부 및 이메일 인증 상태에 따라 `AuthStack`과 `AppStack`을 분기하는 것이 원래 의도였다면, 해당 로직의 주석을 해제하고 `user.emailVerified` 상태가 `AuthContext`를 통해 정확히 관리되고 있는지 확인해야 합니다. (현재 `User` 타입에 `emailVerified`가 있지만, `signIn`이나 `refreshUser` 응답에서 이 값을 설정하는 부분이 명시적으로 보이지 않아 추가 확인 필요)

##### AnalyzeScreen의 TODO 주석
*   **위치:** `CBT-front/src/screens/main/AnalyzeScreen.tsx`
*   **내용:** `"{/* ⚠️ TODO: postId로 제목/내용을 따로 가져오고 싶다면 ... */}"` 와 같은 TODO 주석과 제목/내용을 표시하는 부분의 플레이스홀더 주석이 있습니다.
*   **제안:** 이는 미구현 기능이라기보다는 화면 구성 중 누락된 부분입니다. 분석 결과 화면에서 원본 일기의 제목과 내용을 표시하는 것은 사용자 경험에 중요하므로, 해당 TODO에 따라 일기 데이터를 가져와 표시하도록 기능을 완성하는 것이 좋습니다.
