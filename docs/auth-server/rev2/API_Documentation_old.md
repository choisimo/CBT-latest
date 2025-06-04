# API 문서

이 문서는 Auth-Server Spring Security 애플리케이션에서 제공하는 REST API에 대한 포괄적인 개요를 제공합니다. 모든 API는 JSON을 통해 사용되도록 설계되었습니다.

**기본 URL**: `https://oss-emotion.nodove.com/v1.2`
**인증**: 보호된 엔드포인트의 경우 전달자 토큰(JWT)
**응답 형식**: JSON

---

## 2. 인증 API

### 2.1. 인증 상태 확인 (테스트 전용)

*   **메서드**: `GET`
*   **엔드포인트**: `/auth_check`
*   **설명**: 현재 요청에 유효한 인증 토큰이 있는지 확인합니다.
*   **인증**: 전달자 토큰 필요
*   **응답 코드**:
    *   `200`: 인증됨
    *   `401`: 권한 없음

### 2.2. JWT 토큰 재발급

*   **메서드**: `POST`
*   **엔드포인트**: `/auth/api/protected/refresh`
*   **설명**: 만료된 액세스 토큰과 유효한 리프레시 토큰을 사용하여 새 액세스 토큰을 재발급합니다.
*   **요청 본문**:
    ```json
    {
      "expiredToken": "string",
      "provider": "string" // 예: "server", "google"
    }
    ```
*   **응답 코드**:
    *   `200`: 토큰 재발급 성공
    *   `401`: 리프레시 토큰이 없거나 유효하지 않음
    *   `406`: 잘못된 요청 또는 Redis에서 리프레시 토큰을 찾을 수 없음
*   **응답 헤더**: `Authorization: Bearer {new_access_token}`
*   **응답 본문**:
    ```json
    {
      "access_token": "new_eyJhbGciOiJIUzUxMiJ9..."
    }
    ```

---

## 3. 사용자 관리 API

### 3.1. 사용자 등록

*   **메서드**: `POST`
*   **엔드포인트**: `/api/public/join`
*   **설명**: 새 사용자를 등록합니다. 이메일 인증 코드가 미리 확인되어야 합니다.
*   **요청 본문**:
    ```json
    {
      "userId": "newUser123",
      "userPw": "password123!",
      "userName": "홍길동",
      "phone": "010-1234-5678",
      "email": "user@example.com",
      "role": "USER", // 기본값: USER
      "birthDate": "1990-01-01",
      "gender": "male",
      "isPrivate": false,
      "profile": "https://zrr.kr/iPHf", // 프로필 이미지 URL
      "code": "A1B2C3D4" // 이메일 인증 코드
    }
    ```
*   **응답 코드**:
    *   `200`: 사용자 등록 성공
    *   `400`: 잘못된 요청
    *   `409`: 충돌 (사용자 ID가 이미 존재함)
    *   `500`: 서버 오류
*   **응답 본문**:
    ```json
    {
      "message": "join successfully"
    }
    ```

### 3.2. 로그인

*   **메서드**: `POST`
*   **엔드포인트**: `/api/auth/login`
*   **설명**: 사용자를 로그인하고 JWT 토큰을 발급합니다.
*   **요청 본문**:
    ```json
    {
      "userId": "newUser123",
      "password": "password123!"
    }
    ```
*   **응답 코드**:
    *   `200`: 로그인 성공
    *   `400`: 잘못된 요청
    *   `401`: 로그인 실패
*   **응답 헤더**: `Authorization: Bearer {access_token}`, `Set-Cookie: refreshToken=xxxxxx; Path=/; Domain=your-cookie-domain.com; HttpOnly; Secure`
*   **응답 본문**:
    ```json
    {
      "access_token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXdVc2VyMTIzIiwicm9sZSI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzE1ODAwMDAwLCJleHAiOjE3MTU4MDE4MDB9.xxxx"
    }
    ```

### 3.3. 프로필 이미지 업로드

*   **메서드**: `POST`
*   **엔드포인트**: `/api/public/profileUpload`
*   **설명**: 사용자 프로필 이미지를 업로드하고 이미지 URL을 반환합니다.
*   **요청 본문**: `multipart/form-data` (프로필 이미지 파일 `profile`)
*   **응답 코드**:
    *   `200`: 업로드 성공
    *   `400`: 잘못된 파일
    *   `500`: 서버 오류
*   **응답 본문**:
    ```json
    {
      "fileName": "https://your-file-server.com/attach/profile/xxxx_profile.jpg"
    }
    ```

### 3.4. 사용자 ID 중복 확인

*   **메서드**: `POST`
*   **엔드포인트**: `/api/public/check/userId/IsDuplicate`
*   **설명**: 제공된 사용자 ID가 이미 사용 중인지 확인합니다.
*   **요청 본문**:
    ```json
    {
      "userId": "newUser123"
    }
    ```
*   **응답 코드**:
    *   `200`: 중복 확인 결과
*   **응답 본문**: `boolean` (true: 중복, false: 사용 가능)

### 3.5. 닉네임 중복 확인

*   **메서드**: `POST`
*   **엔드포인트**: `/api/public/check/nickname/IsDuplicate`
*   **설명**: 제공된 닉네임이 이미 사용 중인지 확인합니다.
*   **요청 본문**:
    ```json
    {
      "nickname": "쾌활한다람쥐"
    }
    ```
*   **응답 코드**:
    *   `200`: 중복 확인 결과
*   **응답 본문**: `boolean` (true: 중복, false: 사용 가능)

### 3.6. 사용자 토큰 쿠키 삭제 (로그아웃)

*   **메서드**: `POST`
*   **엔드포인트**: `/api/public/clean/userTokenCookie`
*   **설명**: 클라이언트의 refreshToken 쿠키를 만료시키고 제거합니다.
*   **응답 코드**:
    *   `200`: 쿠키 삭제 성공
*   **응답 본문**:
    ```json
    {
      "message": "User token cookie cleaned successfully"
    }
    ```

---

## 5. 이메일 API

### 5.1. 이메일 인증 코드 발송

*   **메서드**: `POST`
*   **엔드포인트**: `/api/public/emailSend`
*   **설명**: 회원가입을 위한 이메일 인증 코드를 발송합니다.
*   **요청 본문**:
    ```json
    {
      "email": "user@example.com"
    }
    ```
*   **응답 코드**:
    *   `200`: 이메일 발송 성공
    *   `400`: 이미 등록된 이메일
    *   `500`: 이메일 발송 실패
*   **응답 본문**:
    ```json
    {
      "message": "A temporary code has been sent to your email"
    }
    ```

### 5.2. 사용자 정의 이메일 발송 (관리자/내부용)

*   **메서드**: `POST`
*   **엔드포인트**: `/api/private/customEmailSend`
*   **설명**: 지정된 제목과 내용으로 수신자에게 사용자 정의 이메일을 발송합니다.
*   **인증**: 전달자 토큰 필요
*   **요청 본문**:
    ```json
    {
      "email": "user@example.com",
      "content": "이메일 내용",
      "title": "이메일 제목"
    }
    ```
*   **응답 코드**:
    *   `200`: 이메일 발송 성공
    *   `400`: 잘못된 요청
    *   `500`: 이메일 발송 실패
*   **응답 본문**:
    ```json
    {
      "message": "custom email send success"
    }
    ```

### 5.3. 이메일 인증 코드 확인

*   **메서드**: `POST`
*   **엔드포인트**: `/api/public/emailCheck`
*   **설명**: 발송된 이메일 인증 코드의 유효성을 확인합니다.
*   **요청 본문**:
    ```json
    {
      "email": "user@example.com",
      "code": "A1B2C3D4"
    }
    ```
*   **응답 코드**:
    *   `202`: 이메일 코드 유효함
    *   `401`: 이메일 코드 유효하지 않음
*   **응답 본문**:
    ```json
    {
      "message": "email code is valid"
    }
    ```

### 5.4. 임시 비밀번호 이메일 발송 (인증된 사용자)

*   **메서드**: `POST`
*   **엔드포인트**: `/api/protected/sendEmailPassword`
*   **설명**: 현재 로그인한 사용자의 이메일로 임시 비밀번호를 발송합니다.
*   **인증**: 전달자 토큰 필요
*   **응답 코드**:
    *   `200`: 임시 비밀번호 발송 성공
    *   `500`: 임시 비밀번호 발송 실패
*   **응답 본문**:
    ```json
    {
      "message": "A temporary password has been sent to your email"
    }
    ```

### 5.5. 사용자 ID로 비밀번호 찾기 및 임시 비밀번호 발송

*   **메서드**: `GET`
*   **엔드포인트**: `/api/public/findPassWithEmail`
*   **설명**: 사용자 ID를 기반으로 등록된 이메일을 찾아 임시 비밀번호를 발송합니다.
*   **요청 본문**:
    ```json
    {
      "userId": "newUser123"
    }
    ```
*   **응답 코드**:
    *   `200`: 임시 비밀번호 발송 성공
    *   `500`: 임시 비밀번호 발송 실패
*   **응답 본문**:
    ```json
    {
      "message": "A temporary password has been sent to your email"
    }

    ```

---

## 6. SSE API

### 6.1. SSE (서버 발송 이벤트) 구독

*   **메서드**: `GET`
*   **엔드포인트**: `/api/protected/sse/subscribe`
*   **설명**: 서버로부터 실시간 이벤트 스트림을 구독합니다.
*   **인증**: 전달자 토큰 필요
*   **요청 헤더**: `Last-Event-ID`: 마지막으로 수신한 이벤트 ID (선택 사항)
*   **응답 코드**:
    *   `200`: SSE 구독 성공
    *   `401`: 인증 실패
    *   `500`: 서버 오류
*   **응답 형식**: `text/event-stream`
*   **응답 예시**:
    ```text
    id: 123
    event: INIT
    data: {"message": "Subscription successful"}

    id: 124
    event: message
    data: {"content": "New notification!"}
    ```

### 6.2. 특정 사용자에게 더미 SSE 데이터 발송 (테스트 전용)

*   **메서드**: `POST`
*   **엔드포인트**: `/api/public/dummyData/{user_id}`
*   **설명**: 지정된 사용자 ID에 대해 SSE 이벤트를 발생시킵니다.
*   **경로 매개변수**: `user_id`: SSE 이벤트를 수신할 사용자의 ID
*   **요청 본문**:
    ```json
    {
      "message": "Hello from server!"
    }
    ```
*   **응답 코드**:
    *   `200`: 더미 데이터 발송 성공
    *   `400`: 잘못된 요청

---

## 7. 일기 API

### 7.1. 일기 생성

*   **메서드**: `POST`
*   **엔드포인트**: `/api/diaries`
*   **설명**: 새 일기 항목을 생성하고 저장합니다.
*   **인증**: 전달자 토큰 필요
*   **요청 본문**:
    ```json
    {
      "title": "오늘의 일기", // 선택 사항
      "content": "오늘은 정말 즐거운 하루였다." // 필수
    }
    ```
*   **응답 코드**:
    *   `201`: 일기 생성 성공
    *   `400`: 잘못된 요청
    *   `401`: 인증 실패
*   **응답 본문**:
    ```json
    {
      "id": 1,
      "userId": 123,
      "title": "오늘의 일기",
      "content": "오늘은 정말 즐거운 하루였다.",
      "createdAt": "2023-05-01T12:00:00Z",
      "updatedAt": "2023-05-01T12:00:00Z"
    }
    ```

### 7.2. 일기 목록 가져오기

*   **메서드**: `GET`
*   **엔드포인트**: `/api/diaries`
*   **설명**: 사용자가 작성한 일기 항목의 페이지네이션된 목록을 검색합니다.
*   **인증**: 전달자 토큰 필요
*   **쿼리 매개변수**: `page`: 페이지 번호 (기본값 0), `size`: 페이지당 항목 수 (기본값 10), `sort`: 정렬 기준 (예: `createdAt,desc`)
*   **응답 코드**:
    *   `200`: 일기 목록 검색 성공
    *   `401`: 인증 실패
*   **응답 본문**:
    ```json
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
    ```

### 7.3. 특정 일기 세부 정보 가져오기

*   **메서드**: `GET`
*   **엔드포인트**: `/api/diaries/{diaryId}`
*   **설명**: ID로 특정 일기 항목의 세부 내용과 관련된 감정 분석 결과를 검색합니다.
*   **인증**: 전달자 토큰 필요
*   **경로 매개변수**: `diaryId`: 검색할 일기의 ID
*   **응답 코드**:
    *   `200`: 일기 세부 정보 검색 성공
    *   `401`: 인증 실패
    *   `403`: 접근 거부됨
    *   `404`: 일기를 찾을 수 없음
*   **응답 본문**:
    ```json
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
    ```

### 7.4. 특정 일기 업데이트

*   **메서드**: `PUT`
*   **엔드포인트**: `/api/diaries/{diaryId}`
*   **설명**: ID로 특정 일기 항목의 제목 또는 내용을 수정합니다.
*   **인증**: 전달자 토큰 필요
*   **경로 매개변수**: `diaryId`: 업데이트할 일기의 ID
*   **요청 본문**:
    ```json
    {
      "title": "수정된 일기 제목", // 선택 사항
      "content": "수정된 일기 내용" // 필수
    }
    ```
*   **응답 코드**:
    *   `200`: 일기 업데이트 성공
    *   `400`: 잘못된 요청
    *   `401`: 인증 실패
    *   `403`: 접근 거부됨
    *   `404`: 일기를 찾을 수 없음
*   **응답 본문**:
    ```json
    {
      "id": 1,
      "userId": 123,
      "title": "수정된 일기 제목",
      "content": "수정된 일기 내용",
      "createdAt": "2023-05-01T12:00:00Z",
      "updatedAt": "2023-05-01T13:30:00Z"
    }
    ```

### 7.5. 특정 일기 삭제

*   **메서드**: `DELETE`
*   **엔드포인트**: `/api/diaries/{diaryId}`
*   **설명**: ID로 특정 일기 항목을 삭제합니다.
*   **인증**: 전달자 토큰 필요
*   **경로 매개변수**: `diaryId`: 삭제할 일기의 ID
*   **응답 코드**:
    *   `204`: 일기 삭제 성공 (내용 없음)
    *   `401`: 인증 실패
    *   `403`: 접근 거부됨
    *   `404`: 일기를 찾을 수 없음

---

## 9. 설정 API

### 9.1. 사용자 설정 가져오기

*   **메서드**: `GET`
*   **엔드포인트**: `/api/settings`
*   **설명**: 현재 사용자의 사용자 정의 설정 및 기본 애플리케이션 설정을 검색합니다.
*   **인증**: 전달자 토큰 필요
*   **응답 코드**:
    *   `200`: 설정 검색 성공
    *   `401`: 인증 실패
*   **응답 본문**:
    ```json
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
        // ... 추가 설정
      ]
    }
    ```

### 9.2. 사용자 설정 업데이트

*   **메서드**: `PUT`
*   **엔드포인트**: `/api/settings`
*   **설명**: 현재 로그인한 사용자의 특정 설정을 수정합니다.
*   **인증**: 전달자 토큰 필요
*   **요청 본문**:
    ```json
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
        // ... 업데이트할 추가 설정
      ]
    }
    ```
*   **응답 코드**:
    *   `200`: 설정 업데이트 성공
    *   `400`: 잘못된 요청
    *   `401`: 인증 실패
*   **응답 본문**:
    ```json
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
    ```
