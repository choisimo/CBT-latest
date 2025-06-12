# API Specifications

This document outlines the API endpoints provided by the Auth-Server backend. Each section details the available operations for a specific controller or functional area.

## Table of Contents

1.  [Email API](#1-email-api)
2.  [Error Handling](#2-error-handling)
3.  [SSE (Server-Sent Events) API](#3-sse-server-sent-events-api)
4.  [User Management API](#4-user-management-api)
5.  [Authentication API](#5-authentication-api)
6.  [Diary API](#6-diary-api)
7.  [Settings API](#7-settings-api)
8.  [Data Models](#8-data-models)

---

## 1. Email API

**Controller**: `EmailController.java`

**Base Path**: `/api` (Note: some endpoints are public, some private, some protected as per individual endpoint specs)

### 1.1. Email 인증 코드 발송

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/emailSend`                                              |
| 설명        | 회원가입 등을 위한 이메일 인증 코드를 발송합니다.                               |
| 요청 본문   | ```json
  {
    "email": "user@example.com"
  }
  ```                                                                 |
| 응답 코드   | 200: 이메일 전송 성공<br>400: 이미 가입된 이메일<br>500: 이메일 전송 실패               |
| 응답 본문   | ```json
  {
    "message": "A temporary code has been sent to your email"
  }
  ```                                                                 |

### 1.2. 커스텀 이메일 발송 (관리자/내부용)

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/private/customEmailSend`                                       |
| 설명        | 지정된 수신자에게 커스텀 제목과 내용으로 이메일을 발송합니다.                        |
| 인증        | Bearer Token 필요                                                    |
| 요청 본문   | ```json
  {
    "email": "user@example.com",
    "content": "이메일 내용",
    "title": "이메일 제목"
  }
  ```                                                                 |
| 응답 코드   | 200: 이메일 전송 성공<br>400: 잘못된 요청<br>500: 이메일 전송 실패               |
| 응답 본문   | ```json
  {
    "message": "custom email send success"
  }
  ```                                                                 |

### 1.3. 이메일 인증 코드 확인

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/emailCheck`                                             |
| 설명        | 발송된 이메일 인증 코드의 유효성을 확인합니다.                                 |
| 요청 본문   | ```json
  {
    "email": "user@example.com",
    "code": "A1B2C3D4"
  }
  ```                                                                 |
| 응답 코드   | 202: 이메일 코드 유효<br>401: 이메일 코드 무효                               |
| 응답 본문   | ```json
  {
    "message": "email code is valid"
  }
  ```                                                                 |

### 1.4. 임시 비밀번호 이메일 전송 (인증된 사용자)

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/protected/sendEmailPassword`                                   |
| 설명        | 현재 로그인된 사용자의 이메일로 임시 비밀번호를 발송합니다.                          |
| 인증        | Bearer Token 필요                                                    |
| 응답 코드   | 200: 임시 비밀번호 전송 성공<br>500: 임시 비밀번호 전송 실패                     |
| 응답 본문   | ```json
  {
    "message": "A temporary password has been sent to your email"
  }
  ```                                                                 |

### 1.5. 아이디로 이메일 찾아 임시 비밀번호 전송

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | GET                                                                  |
| 엔드포인트  | `/api/public/findPassWithEmail`                                      |
| 설명        | 사용자 ID를 기반으로 등록된 이메일을 찾아 임시 비밀번호를 발송합니다.                    |
| 요청 본문   | ```json
  {
    "userId": "newUser123"
  }
  ```                                                                 |
| 응답 코드   | 200: 임시 비밀번호 전송 성공<br>500: 임시 비밀번호 전송 실패                     |
| 응답 본문   | ```json
  {
    "message": "A temporary password has been sent to your email"
  }
  ```                                                                 |

---

## 2. Error Handling

**Controller**: Spring Boot's `BasicErrorController` (as `com.authentication.auth.controller.ErrorController.java` is currently commented out).

**Description**: The application utilizes Spring Boot's default error handling mechanism. When an error occurs, Spring Boot's `BasicErrorController` processes it and typically returns a JSON response structured according to the `ErrorResponse` data model (see Data Models section for fields like `timestamp`, `status`, `error`, `message`, `path`).

While the custom `ErrorController.java` in the codebase defines specific template-based responses for errors like 404 and 401, this controller is currently commented out. Therefore, these custom HTML error pages (`notExist.html`, `unauthorized.html`) will not be rendered. Instead, standard Spring Boot error responses will be generated.

---

## 3. SSE (Server-Sent Events) API

**Controller**: `SseController.java`

**Base Path**: `/api` (Note: Endpoints are typically under `/api/protected/sse` or `/api/public/dummyData` as specified)

### 3.1. SSE (Server-Sent Events) 구독

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | GET                                                                  |
| 엔드포인트  | `/api/protected/sse/subscribe`                                       |
| 설명        | 서버로부터 실시간 이벤트 스트림을 구독합니다.                                  |
| 인증        | Bearer Token 필요                                                    |
| 요청 헤더   | Last-Event-ID: 마지막으로 수신한 이벤트 ID (선택적)                           |
| 응답 코드   | 200: SSE 구독 성공<br>401: 인증 실패<br>500: 서버 오류                       |
| 응답 형식   | text/event-stream                                                    |
| 응답 예시   | ```text
  id: 123
  event: INIT
  data: {"message": "Subscription successful"}

  id: 124
  event: message
  data: {"content": "New notification!"}
  ```                                                                 |

### 3.2. 특정 사용자에게 더미 SSE 데이터 전송 (테스트용)

| 항목          | 설명                                                                 |
| ------------- | -------------------------------------------------------------------- |
| 메소드        | POST                                                                 |
| 엔드포인트    | `/api/public/dummyData/{user_id}`                                    |
| 설명          | 지정된 사용자 ID에게 SSE 이벤트를 발생시킵니다.                               |
| 경로 파라미터 | user_id: SSE 이벤트를 수신할 사용자의 ID                                 |
| 요청 본문     | ```json
  {
    "message": "Hello from server!"
  }
  ```                                                                 |
| 응답 코드     | 200: 더미 데이터 전송 성공<br>400: 잘못된 요청                               |

---

## 4. User Management API

**Controller**: `UsersController.java`

**Base Path**: `/api` (Endpoints are typically under `/api/public` or `/api/protected` as specified)

### 4.1. 회원 가입

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/join`                                                   |
| 설명        | 새로운 사용자를 등록합니다. 이메일 인증 코드가 사전에 검증되어야 합니다.               |
| 요청 본문   | ```json
  {
    "userId": "newUser123",
    "userPw": "password123!",
    "userName": "홍길동",
    "nickname": "쾌활한다람쥐",
    "phone": "010-1234-5678",
    "email": "user@example.com",
    "role": "USER", // 기본값: USER
    "birthDate": "1990-01-01",
    "gender": "male",
    "isPrivate": false,
    "profile": "https://zrr.kr/iPHf", // 프로필 이미지 URL
    "code": "A1B2C3D4" // 이메일 인증 코드
  }
  ```                                                                 |
| 응답 코드   | 200: 회원 가입 성공<br>400: 잘못된 요청<br>409: 충돌 (이미 존재하는 아이디/닉네임)<br>500: 서버 오류 |
| 응답 본문   | ```json
  {
    "message": "join successfully"
  }
  ```                                                                 |

### 4.2. 로그인

*Note: While `/api/auth/login` is listed under User Management in `user_management_api.md`, it semantically fits better under the Authentication API section (section 5) as it deals with the core authentication process and token issuance. It will be documented there.* *The `user_management_api.md` also lists `/api/auth/login` which is handled by `AuthController`.* 

### 4.3. 프로필 이미지 업로드

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/profileUpload`                                          |
| 설명        | 사용자 프로필 이미지를 업로드하고 이미지 URL을 반환받습니다.                         |
| 요청 본문   | multipart/form-data 형식<br>profile: 이미지 파일                        |
| 응답 코드   | 200: 업로드 성공<br>400: 잘못된 파일<br>500: 서버 오류                          |
| 응답 본문   | ```json
  {
    "fileName": "https://your-file-server.com/attach/profile/xxxx_profile.jpg"
  }
  ```                                                                 |

### 4.4. 사용자 ID 중복 체크

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/check/userId/IsDuplicate`                               |
| 설명        | 제공된 사용자 ID가 이미 사용 중인지 확인합니다.                                |
| 요청 본문   | ```json
  {
    "userId": "newUser123"
  }
  ```                                                                 |
| 응답 코드   | 200: 중복 체크 결과                                                      |
| 응답 본문   | boolean (true: 중복됨, false: 사용 가능)                                 |

### 4.5. 닉네임 중복 체크

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/check/nickname/IsDuplicate`                             |
| 설명        | 제공된 닉네임이 이미 사용 중인지 확인합니다.                                 |
| 요청 본문   | ```json
  {
    "nickname": "쾌활한다람쥐"
  }
  ```                                                                 |
| 응답 코드   | 200: 중복 체크 결과                                                      |
| 응답 본문   | boolean (true: 중복됨, false: 사용 가능)                                 |

### 4.6. 사용자 토큰 쿠키 정리 (로그아웃)

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/clean/userTokenCookie`                                  |
| 설명        | 클라이언트의 refreshToken 쿠키를 만료시켜 제거합니다. (소프트 로그아웃)        |
| 응답 코드   | 200: 쿠키 정리 성공                                                     |
| 응답 본문   | ```json
  {
    "message": "User token cookie cleared successfully"
  }
  ```                                                                 |

---

## 5. Authentication API

**Controllers**: `AuthController.java`, `TokenController.java`, `Oauth2Controller.java`

**Base Path**: `/api/auth`, `/auth`, or `/oauth2` (as specified by individual endpoints)

### 5.1. 로그인 (JWT 발급)

*Source: `user_management_api.md` (Handled by Spring Security, likely via `UsernamePasswordAuthenticationFilter` or custom JWT filter processing `/api/auth/login`)*

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/auth/login`                                                    |
| 설명        | 사용자 ID와 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.                         |
| 요청 본문   | ```json
  {
    "userId": "newUser123",
    "password": "password123!"
  }
  ```                                                                 |
| 응답 코드   | 200: 로그인 성공<br>400: 잘못된 요청<br>401: 로그인 실패                          |
| 응답 헤더   | Authorization: Bearer {액세스 토큰}<br>Set-Cookie: refreshToken=xxxxxx; Path=/; Domain=your-cookie-domain.com; HttpOnly; Secure | // Note: Domain should be configured
| 응답 본문   | ```json
  {
    "access_token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXdVc2VyMTIzIiwicm9sZSI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzE1ODAwMDAwLCJleHAiOjE3MTU4MDE4MDB9.xxxx"
  }
  ```                                                                 |

### 5.2. 인증 상태 확인 (테스트용)

*Source: `auth_api.md` (handled by `AuthController.java`)*

| 항목        | 설명                                         |
| ----------- | -------------------------------------------- |
| 메소드      | GET                                          |
| 엔드포인트  | `/auth_check`                                | 
| 설명        | 현재 요청이 유효한 인증 토큰을 가지고 있는지 확인합니다. | 
| 인증        | Bearer Token 필요                            |
| 응답 코드   | 200: 인증됨<br>401: 인증되지 않음             |

### 5.3. JWT 토큰 재발급

*Source: `auth_api.md` (handled by `TokenController.java`)*

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/auth/api/protected/refresh`                                        |
| 설명        | 만료된 Access Token과 유효한 Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. |
| 요청 본문   | ```json
  {
    "expiredToken": "string",
    "provider": "string" // 예: "server", "google"
  }
  ```                                                                 |
| 응답 코드   | 200: 토큰 재발급 성공<br>401: Refresh Token이 없거나 유효하지 않음<br>406: 요청이 유효하지 않거나 Redis에 Refresh Token이 없음 |
| 응답 헤더   | Authorization: Bearer {새로운 액세스 토큰}                                   |
| 응답 본문   | ```json
  {
    "access_token": "new_eyJhbGciOiJIUzUxMiJ9..."
  }
  ```                                                                 |

### 5.4. OAuth2 Authentication

*Source: `oauth2_api.md` (handled by `Oauth2Controller.java`)*

The following endpoints manage the OAuth2 authentication flow.

#### 5.4.1. Get OAuth2 Login URL

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | GET                                                                  |
| 엔드포인트  | `/api/public/oauth2/login_url/{provider}`                            |
| 설명        | 지정된 OAuth2 제공업체(provider)의 로그인 페이지로 리디렉션할 URL을 가져옵니다. 사용자는 이 URL로 이동하여 인증을 시작합니다. |
| 경로 파라미터 | `provider`: 문자열. OAuth2 제공업체 이름 (예: "google", "kakao", "naver"). `AuthProvider.ProviderType` 에 정의된 값 중 하나여야 합니다. |
| 응답 코드   | 200: 성공<br>400: 잘못된 제공업체 이름                               |
| 응답 본문   | ```json
  {
    "login_url": "https://accounts.google.com/o/oauth2/v2/auth?client_id=..."
  }
  ```                                                                 |
| 응답 DTO    | `OAuth2LoginUrlResponse` (fields: `login_url`)                      |

#### 5.4.2. Handle OAuth2 Callback

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | GET                                                                  |
| 엔드포인트  | `/api/public/oauth2/callback/{provider}`                             |
| 설명        | OAuth2 제공업체로부터의 인증 콜백을 처리합니다. 인증 성공 시 JWT 토큰과 사용자 프로필을 반환합니다. |
| 경로 파라미터 | `provider`: 문자열. OAuth2 제공업체 이름.                               |
| 쿼리 파라미터 | `code` (또는 `tempCode`): 문자열. 제공업체로부터 받은 인증 코드.<br>`state`: 문자열. CSRF 방지를 위한 상태 값 (선택적, 제공업체에 따라 다름). |
| 요청 DTO    | `OAuth2CallbackRequest` (fields: `tempCode`, `state`) - 쿼리 파라미터로 전달됨 |
| 응답 코드   | 200: 로그인 성공<br>400: 잘못된 요청 또는 코드/상태 값<br>401: 인증 실패<br>500: 서버 오류 |
| 응답 헤더   | `Authorization`: `Bearer {액세스 토큰}`<br>`Set-Cookie`: `refreshToken=xxxxxx; Path=/; Domain=your-cookie-domain.com; HttpOnly; Secure` |
| 응답 본문   | ```json
  {
    "access_token": "eyJhbGciOiJIUzUxMiJ9...",
    "userProfile": {
      "nickname": "쾌활한다람쥐",
      "email": "user@example.com",
      "profileImageUrl": "https://example.com/profile.jpg"
    }
  }
  ```                                                                 |
| 응답 DTO    | `OAuth2LoginResponse` (fields: `access_token`, `userProfile`)         |

**Controller**: `Oauth2Controller.java`

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |

### 5.5. OAuth2 Kakao 로그인 콜백

**Controller**: `Oauth2Controller.java`

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/oauth2/callback/kakao`                                             |
| 설명        | Kakao OAuth2 인증 후, 코드를 받아 서버에서 로그인/회원가입 처리 및 JWT를 발급합니다. |
| 요청 본문   | ```json
  {
    "tempCode": "authorization_code_from_kakao"
    // Potentially other parameters depending on Kakao's response and server-side needs
  }
  ```                                                                 |
| 응답 코드   | 200: OAuth2 로그인/회원가입 성공, JWT 발급<br>400: 잘못된 요청 또는 코드<br>500: 서버 오류 |
| 응답 헤더   | Authorization: Bearer {액세스 토큰}<br>Set-Cookie: refreshToken=... (similar to standard login) |
| 응답 본문   | ```json
  {
    "access_token": "eyJhbGciOiJIUzUxMiJ9...",
    "userProfile": { /* 사용자 프로필 정보 */ }
  }
  ```                                                                 |

### 5.6. OAuth2 Naver 로그인 콜백

**Controller**: `Oauth2Controller.java`

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/oauth2/callback/naver`                                             |
| 설명        | Naver OAuth2 인증 후, 코드를 받아 서버에서 로그인/회원가입 처리 및 JWT를 발급합니다. |
| 요청 본문   | ```json
  {
    "tempCode": "authorization_code_from_naver",
    "state": "csrf_token_from_naver" // Naver requires state parameter
    // Potentially other parameters
  }
  ```                                                                 |
| 응답 코드   | 200: OAuth2 로그인/회원가입 성공, JWT 발급<br>400: 잘못된 요청, 코드 또는 state 불일치<br>500: 서버 오류 |
| 응답 헤더   | Authorization: Bearer {액세스 토큰}<br>Set-Cookie: refreshToken=... |
| 응답 본문   | ```json
  {
    "access_token": "eyJhbGciOiJIUzUxMiJ9...",
    "userProfile": { /* 사용자 프로필 정보 */ }
  }
  ```                                                                 |

### 5.7. OAuth2 Google 로그인 콜백

**Controller**: `Oauth2Controller.java`

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/oauth2/callback/google`                                            |
| 설명        | Google OAuth2 인증 후, 코드를 받아 서버에서 로그인/회원가입 처리 및 JWT를 발급합니다. |
| 요청 본문   | ```json
  {
    "tempCode": "authorization_code_from_google"
    // Potentially other parameters like scope, id_token depending on Google's flow
  }
  ```                                                                 |
| 응답 코드   | 200: OAuth2 로그인/회원가입 성공, JWT 발급<br>400: 잘못된 요청 또는 코드<br>500: 서버 오류 |
| 응답 헤더   | Authorization: Bearer {액세스 토큰}<br>Set-Cookie: refreshToken=... |
| 응답 본문   | ```json
  {
    "access_token": "eyJhbGciOiJIUzUxMiJ9...",
    "userProfile": { /* 사용자 프로필 정보 */ }
  }
  ```                                                                 |

---

## 6. Diary API

**Controller**: `DiaryController.java`

**Base Path**: `/api/diaries`

### 6.1. 일기 작성

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/diaries`                                                       |
| 설명        | 새로운 일기를 작성하고 저장합니다.                                          |
| 인증        | Bearer Token 필요                                                    |
| 요청 본문   | ```json
  {
    "title": "오늘의 일기", // 선택적
    "content": "오늘은 정말 즐거운 하루였다." // 필수
  }
  ```                                                                 |
| 응답 코드   | 201: 일기 작성 성공<br>400: 잘못된 요청<br>401: 인증 실패                      |
| 응답 본문   | ```json
  {
    "id": 1,
    "userId": 123,
    "title": "오늘의 일기",
    "content": "오늘은 정말 즐거운 하루였다.",
    "createdAt": "2023-05-01T12:00:00Z",
    "updatedAt": "2023-05-01T12:00:00Z"
  }
  ```                                                                 |

### 6.2. 일기 목록 조회

| 항목          | 설명                                                                 |
| ------------- | -------------------------------------------------------------------- |
| 메소드        | GET                                                                  |
| 엔드포인트    | `/api/diaries`                                                       |
| 설명          | 사용자가 작성한 일기 목록을 페이지네이션하여 조회합니다.                           |
| 인증          | Bearer Token 필요                                                    |
| 쿼리 파라미터 | page: 페이지 번호 (기본값 0)<br>size: 페이지 당 항목 수 (기본값 10)<br>sort: 정렬 기준 (예: createdAt,desc) |
| 응답 코드     | 200: 일기 목록 조회 성공<br>401: 인증 실패                               |
| 응답 본문     | ```json
  {
    "diaries": [
      {
        "id": 1,
        "title": "오늘의 일기",
        "createdAt": "2023-05-01T12:00:00Z",
        "emotionStatus": "POSITIVE"
      }
      // ... 추가 일기 항목
    ],
    "pageInfo": {
      "currentPage": 0,
      "totalPages": 5,
      "totalElements": 42
    }
  }
  ```                                                                 |

### 6.3. 특정 일기 상세 조회

| 항목          | 설명                                                                 |
| ------------- | -------------------------------------------------------------------- |
| 메소드        | GET                                                                  |
| 엔드포인트    | `/api/diaries/{diaryId}`                                             |
| 설명          | ID에 해당하는 특정 일기의 상세 내용과 연관된 감정 분석 결과를 함께 조회합니다.           |
| 인증          | Bearer Token 필요                                                    |
| 경로 파라미터 | diaryId: 조회할 일기의 ID                                              |
| 응답 코드     | 200: 일기 상세 조회 성공<br>401: 인증 실패<br>403: 접근 권한 없음<br>404: 일기를 찾을 수 없음 |
| 응답 본문     | ```json
  {
    "id": 1,
    "userId": 123,
    "title": "오늘의 일기",
    "content": "오늘은 정말 즐거운 하루였다.",
    "alternativeThoughtByAI": "오늘은 새로운 경험을 통해 성장한 하루였다.",
    "createdAt": "2023-05-01T12:00:00Z",
    "updatedAt": "2023-05-01T12:00:00Z",
    "analysis": {
      "id": 42,
      "emotionDetection": "기쁨 80%, 슬픔 10%, 놀람 5%, 평온 5%",
      "automaticThought": "나는 항상 실패한다.",
      "promptForChange": "정말 항상 실패했나요? 성공했던 경험을 떠올려볼까요?",
      "alternativeThought": "과거에 몇 번 실패했지만, 그것이 항상 실패한다는 의미는 아니다. 이번에는 다른 방법을 시도해볼 수 있다.",
      "status": "POSITIVE",
      "analyzedAt": "2023-05-01T12:05:00Z"
    }
  }
  ```                                                                 |

### 6.4. 특정 일기 수정

| 항목          | 설명                                                                 |
| ------------- | -------------------------------------------------------------------- |
| 메소드        | PUT                                                                  |
| 엔드포인트    | `/api/diaries/{diaryId}`                                             |
| 설명          | ID에 해당하는 특정 일기의 제목 또는 내용을 수정합니다.                             |
| 인증          | Bearer Token 필요                                                    |
| 경로 파라미터 | diaryId: 수정할 일기의 ID                                              |
| 요청 본문     | ```json
  {
    "title": "수정된 일기 제목", // 선택적
    "content": "수정된 일기 내용" // 필수
  }
  ```                                                                 |
| 응답 코드     | 200: 일기 수정 성공<br>400: 잘못된 요청<br>401: 인증 실패<br>403: 접근 권한 없음<br>404: 일기를 찾을 수 없음 |
| 응답 본문     | ```json
  {
    "id": 1,
    "userId": 123,
    "title": "수정된 일기 제목",
    "content": "수정된 일기 내용",
    "createdAt": "2023-05-01T12:00:00Z",
    "updatedAt": "2023-05-01T13:30:00Z"
  }
  ```                                                                 |

### 6.5. 특정 일기 삭제

| 항목          | 설명                                                                 |
| ------------- | -------------------------------------------------------------------- |
| 메소드        | DELETE                                                               |
| 엔드포인트    | `/api/diaries/{diaryId}`                                             |
| 설명          | ID에 해당하는 특정 일기를 삭제합니다.                                      |
| 인증          | Bearer Token 필요                                                    |
| 경로 파라미터 | diaryId: 삭제할 일기의 ID                                              |
| 응답 코드     | 204: 일기 삭제 성공 (No Content)<br>401: 인증 실패<br>403: 접근 권한 없음<br>404: 일기를 찾을 수 없음 |

---

## 7. Settings API

**Controller**: `SettingsController.java` (Assumed, verify if different)

**Base Path**: `/api/settings`

### 7.1. 사용자 설정 조회

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | GET                                                                  |
| 엔드포인트  | `/api/settings`                                                      |
| 설명        | 현재 로그인된 사용자의 커스텀 설정 및 기본 애플리케이션 설정을 조회합니다.             |
| 인증        | Bearer Token 필요                                                    |
| 응답 코드   | 200: 설정 조회 성공<br>401: 인증 실패                                  |
| 응답 본문   | ```json
  {
    "settings": [
      {
        "settingKey": "notification.enabled",
        "value": true,
        "dataType": "BOOLEAN",
        "description": "알림 활성화 여부",
        "isUserEditable": true
      },
      {
        "settingKey": "theme.color",
        "value": "dark",
        "dataType": "STRING",
        "description": "앱 테마 색상",
        "isUserEditable": true
      }
      // ... 추가 설정 항목
    ]
  }
  ```                                                                 |

### 7.2. 사용자 설정 수정

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | PUT                                                                  |
| 엔드포인트  | `/api/settings`                                                      |
| 설명        | 현재 로그인된 사용자의 특정 설정을 수정합니다.                                 |
| 인증        | Bearer Token 필요                                                    |
| 요청 본문   | ```json
  {
    "settingsToUpdate": [
      {
        "settingKey": "notification.enabled",
        "newValue": false
      },
      {
        "settingKey": "theme.color",
        "newValue": "light"
      }
      // ... 추가 수정할 설정
    ]
  }
  ```                                                                 |
| 응답 코드   | 200: 설정 수정 성공<br>400: 잘못된 요청<br>401: 인증 실패                      |
| 응답 본문   | ```json
  {
    "message": "Settings updated successfully",
    "updatedSettings": [
      {
        "settingKey": "notification.enabled",
        "value": false
      },
      {
        "settingKey": "theme.color",
        "value": "light"
      }
    ]
  }
  ```                                                                 |

---

## 8. Data Models

This section outlines the primary data models used for requests and responses across the various APIs. For detailed field descriptions, refer to the corresponding Java DTO classes within the project.

*Implementation Guideline: All data model Repository implementations should use JPA (Java Persistence API) and QueryDSL (specifically, the `io.github.openfeign.querydsl` fork). Direct SQL query writing is discouraged to maintain type safety, readability, and maintainability.*

### 8.1. Authentication and User Models

| Model                 | Description        | Key Fields                                                                 |
| --------------------- | ------------------ | -------------------------------------------------------------------------- |
| `JoinRequest`         | 회원가입 요청      | `userId`, `userPw`, `userName`, `nickname`, `phone`, `gender`, `code` (email auth code), etc. |
| `LoginRequest`        | 로그인 요청        | `userId`, `password`                                                       |
| `LoginResponse`       | 로그인 응답        | `access_token`                                                             |
| `TokenRefreshRequest` | 토큰 갱신 요청     | `expiredToken`, `provider`                                                 |
| `TokenRefreshResponse`| 토큰 갱신 응답     | `access_token`                                                             |
| `OAuth2CallbackRequest`| OAuth2 콜백 요청  | `tempCode`, `state`                                                        |
| `OAuth2LoginUrlResponse`| OAuth2 로그인 URL 응답 | `login_url`                                                              |
| `OAuth2LoginResponse` | OAuth2 로그인 응답 | `access_token`, `userProfile`                                              |

### 8.2. Email Models

| Model                 | Description        | Key Fields                |
| --------------------- | ------------------ | ------------------------- |
| `EmailRequest`        | 이메일 요청        | `email`                   |
| `EmailSendResponse`   | 이메일 전송 응답   | `message`                 |
| `CustomEmailRequest`  | 커스텀 이메일 요청 | `email`, `content`, `title` |
| `EmailCheckDto`       | 이메일 확인 요청   | `email`, `code`           |
| `EmailCheckResponse`  | 이메일 확인 응답   | `message`                 |

### 8.3. Diary and Analysis Models

| Model                          | Description        | Key Fields                                                               |
| ------------------------------ | ------------------ | ------------------------------------------------------------------------ |
| `DiaryCreateRequest`           | 일기 생성 요청     | `title`, `content`                                                       |
| `DiaryResponse`                | 일기 응답          | `id`, `userId`, `title`, `content`, `createdAt`, `updatedAt`             |
| `DiaryUpdateRequest`           | 일기 수정 요청     | `title`, `content`                                                       |
| `DiaryListItem`                | 일기 목록 항목     | `id`, `title`, `createdAt`, `emotionStatus`                              |
| `DiaryDetailResponse`          | 일기 상세 응답     | `id`, `userId`, `title`, `content`, `alternativeThoughtByAI`, `createdAt`, `updatedAt`, `analysis` |
| `DiaryAnalysisResult`          | 일기 분석 결과     | `id`, `emotionDetection`, `automaticThought`, `promptForChange`, `alternativeThought`, `status`, `analyzedAt` |
| `DiaryAnalysisRequestResponse` | 일기 분석 요청 응답 | `message`, `diaryId`, `trackingId`                                       |

### 8.4. Settings Models

| Model                       | Description          | Key Fields                                                        |
| --------------------------- | -------------------- | ----------------------------------------------------------------- |
| `SettingItem`               | 설정 항목            | `settingKey`, `value`, `dataType`, `description`, `isUserEditable` |
| `SettingsListResponse`      | 설정 목록 응답       | `settings`                                                        |
| `SettingsUpdateRequestItem` | 설정 수정 요청 항목  | `settingKey`, `newValue`                                          |
| `SettingsUpdateRequest`     | 설정 수정 요청       | `settingsToUpdate`                                                |
| `SettingsUpdateResponse`    | 설정 수정 응답       | `message`, `updatedSettings`                                      |

### 8.5. Error Models

| Model           | Description | Key Fields                                     |
| --------------- | ----------- | ---------------------------------------------- |
| `ErrorResponse` | 오류 응답   | `timestamp`, `status`, `error`, `message`, `path` |

---
