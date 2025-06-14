# API 명세서

이 문서는 Auth-Server 백엔드에서 제공하는 API 엔드포인트를 설명합니다. 각 섹션은 특정 컨트롤러 또는 기능 영역에 사용할 수 있는 작업을 자세히 설명합니다.

## 목차

1.  [이메일 API](#1-이메일-api)
2.  [오류 처리](#2-오류-처리)
3.  [SSE (Server-Sent Events) API](#3-sse-server-sent-events-api)
4.  [사용자 관리 API](#4-사용자-관리-api)
5.  [인증 API](#5-인증-api)
6.  [관리자 필터 관리 API](#6-관리자-필터-관리-api)
7.  [데이터 모델](#7-데이터-모델)

---

## 1. 이메일 API

**컨트롤러**: `EmailController.java`

**기본 경로**: `/api` (참고: 일부 엔드포인트는 공개, 일부는 비공개, 일부는 개별 엔드포인트 사양에 따라 보호됨)

### 1.1. Email 인증 코드 발송

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/emailSend`                                              |
| 설명        | 회원가입 등을 위한 이메일 인증 코드를 발송합니다.                               |
| 요청 본문   | { "email": "user@example.com" }                                   |
| 응답 코드   | 200: 이메일 전송 성공<br>400: 이미 가입된 이메일<br>500: 이메일 전송 실패               |
| 응답 본문   | { "message": "A temporary code has been sent to your email" }     |

**요청 본문 예시**
```json
{
  "email": "user@example.com"
}
```

**응답 본문 예시**
```json
{
  "message": "A temporary code has been sent to your email"
}
```

### 1.2. 커스텀 이메일 발송 (관리자/내부용)

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/private/customEmailSend`                                       |
| 설명        | 지정된 수신자에게 커스텀 제목과 내용으로 이메일을 발송합니다.                        |
| 인증        | Bearer Token 필요                                                    |
| 요청 본문   | { "email": "user@example.com", "content": "이메일 내용", "title": "이메일 제목" } |
| 응답 코드   | 200: 이메일 전송 성공<br>400: 잘못된 요청<br>500: 이메일 전송 실패               |
| 응답 본문   | { "message": "custom email send success" }                             |

**요청 본문 예시**
```json
{
  "email": "user@example.com",
  "content": "이메일 내용",
  "title": "이메일 제목"
}
```

**응답 본문 예시**
```json
{
  "message": "custom email send success"
}
```

### 1.3. 이메일 인증 코드 확인

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/emailCheck`                                             |
| 설명        | 발송된 이메일 인증 코드의 유효성을 확인합니다.                                 |
| 요청 본문   | { "email": "user@example.com", "code": "A1B2C3D4" }                     |
| 응답 코드   | 202: 이메일 코드 유효<br>401: 이메일 코드 무효                               |
| 응답 본문   | { "message": "email code is valid" }                                   |

**요청 본문 예시**
```json
{
  "email": "user@example.com",
  "code": "A1B2C3D4"
}
```

**응답 본문 예시**
```json
{
  "message": "email code is valid"
}
```

### 1.4. 임시 비밀번호 이메일 전송 (인증된 사용자)

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/protected/sendEmailPassword`                                   |
| 설명        | 현재 로그인된 사용자의 이메일로 임시 비밀번호를 발송합니다.                          |
| 인증        | Bearer Token 필요                                                    |
| 응답 코드   | 200: 임시 비밀번호 전송 성공<br>500: 임시 비밀번호 전송 실패                     |
| 응답 본문   | { "message": "A temporary password has been sent to your email" }     |

**요청 본문 예시**
```json
{
  "message": "A temporary password has been sent to your email"
}
```

### 1.5. 아이디로 이메일 찾아 임시 비밀번호 전송

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/findPassWithEmail`                                      |
| 설명        | 사용자 ID를 기반으로 등록된 이메일을 찾아 임시 비밀번호를 발송합니다.                    |
| 요청 본문   | { "userId": "newUser123" }                                           |
| 응답 코드   | 200: 임시 비밀번호 전송 성공<br>500: 임시 비밀번호 전송 실패                     |
| 응답 본문   | { "message": "A temporary password has been sent to your email" }     |

**요청 본문 예시**
```json
{
  "userId": "newUser123"
}
```

**응답 본문 예시**
```json
{
  "message": "A temporary password has been sent to your email"
}
```

---

## 2. 오류 처리

**컨트롤러**: Spring Boot의 `BasicErrorController` (`com.authentication.auth.controller.ErrorController.java`가 현재 주석 처리되어 있으므로).

**설명**: 애플리케이션은 Spring Boot의 기본 오류 처리 메커니즘을 활용합니다. 오류가 발생하면 Spring Boot의 `BasicErrorController`가 이를 처리하고 일반적으로 `ErrorResponse` 데이터 모델에 따라 구조화된 JSON 응답을 반환합니다 (필드(`timestamp`, `status`, `error`, `message`, `path` 등)는 데이터 모델 섹션 참조).

코드베이스의 사용자 정의 `ErrorController.java`는 404 및 401과 같은 오류에 대한 특정 템플릿 기반 응답을 정의하지만 이 컨트롤러는 현재 주석 처리되어 있습니다. 따라서 이러한 사용자 정의 HTML 오류 페이지(`notExist.html`, `unauthorized.html`)는 렌더링되지 않습니다. 대신 표준 Spring Boot 오류 응답이 생성됩니다.

일반적인 HTTP 오류 응답은 다음과 같습니다.

#### 2.1. 401 Unauthorized (권한 없음)

이 오류는 요청에 HTTP 인증이 필요하며 실패했거나 아직 제공되지 않았음을 나타냅니다.

**일반적인 원인:**

*   보호된 엔드포인트에 대한 요청에 `Authorization` 헤더가 없음.
*   유효하지 않거나, 잘못되었거나, 만료된 JWT 토큰 제공.
*   인증 없이 로그인이 필요한 리소스에 액세스하려고 시도.

**JSON 응답 예시:**

```json
{
  "timestamp": "2023-08-15T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication token is missing or invalid",
  "path": "/api/protected/resource"
}
```

#### 2.2. 403 Forbidden (금지됨)

이 오류는 서버가 요청을 이해했지만 권한 부여를 거부함을 나타냅니다. 401 Unauthorized 응답과 달리 인증해도 아무런 차이가 없습니다.

**일반적인 원인:**

*   인증된 사용자에게 요청된 리소스에 액세스하거나 요청된 작업을 수행하는 데 필요한 역할이나 권한이 없습니다. 예를 들어 `ROLE_USER` 권한을 가진 사용자가 `ROLE_ADMIN`으로 제한된 엔드포인트에 액세스하려고 하는 경우입니다.

**JSON 응답 예시:**

```json
{
  "timestamp": "2023-08-15T10:35:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "User does not have the required permissions: Requires ROLE_ADMIN",
  "path": "/admin/some-admin-resource"
}
```

---

## 3. SSE (Server-Sent Events) API

**컨트롤러**: `SseController.java`

**기본 경로**: `/api` (참고: 엔드포인트는 일반적으로 지정된 대로 `/api/protected/sse` 또는 `/api/public/dummyData` 아래에 있음)

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
| 응답 예시   | { "id": 123, "event": "INIT", "data": { "message": "Subscription successful" } }
  { "id": 124, "event": "message", "data": { "content": "New notification!" } }
```                                                                 |

**요청 본문 예시**
```json
{
  "id": 123,
  "event": "INIT",
  "data": {
    "message": "Subscription successful"
  }
}
```

**응답 본문 예시**
```json
{
  "id": 124,
  "event": "message",
  "data": {
    "content": "New notification!"
  }
}
```

### 3.2. 특정 사용자에게 더미 SSE 데이터 전송 (테스트용)

| 항목          | 설명                                                                 |
| ------------- | -------------------------------------------------------------------- |
| 메소드        | POST                                                                 |
| 엔드포인트    | `/api/public/dummyData/{user_id}`                                    |
| 설명          | 지정된 사용자 ID에게 SSE 이벤트를 발생시킵니다.                               |
| 경로 파라미터 | user_id: SSE 이벤트를 수신할 사용자의 ID                                 |
| 요청 본문     | { "message": "Hello from server!" }                                   |
| 응답 코드     | 200: 더미 데이터 전송 성공<br>400: 잘못된 요청                               |

**요청 본문 예시**
```json
{
  "message": "Hello from server!"
}
```

**응답 본문 예시**
```json
{
  "message": "Hello from server!"
}
```

---

## 4. 사용자 관리 API

**컨트롤러**: `UsersController.java`

**기본 경로**: `/api` (엔드포인트는 일반적으로 지정된 대로 `/api/public` 또는 `/api/protected` 아래에 있음)

### 4.1. 회원 가입

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/join`                                                   |
| 설명        | 새로운 사용자를 등록합니다. 이메일 인증 코드가 사전에 검증되어야 합니다.               |
| 요청 본문   | { "userId": "newUser123", "userPw": "password123!", "userName": "홍길동", "nickname": "쾌활한다람쥐", "phone": "010-1234-5678", "email": "user@example.com", "role": "USER", "birthDate": "1990-01-01", "gender": "male", "isPrivate": false, "profile": "https://zrr.kr/iPHf", "code": "A1B2C3D4" } |
| 응답 코드   | 200: 회원 가입 성공<br>400: 잘못된 요청<br>409: 충돌 (이미 존재하는 아이디/닉네임)<br>500: 서버 오류 |
| 응답 본문   | { "status": "success", "message": "회원가입이 성공적으로 완료되었습니다.", "data": null }                                     |

**요청 본문 예시**
```json
{
  "userId": "newUser123",
  "userPw": "password123!",
  "userName": "홍길동",
  "nickname": "쾌활한다람쥐",
  "phone": "010-1234-5678",
  "email": "user@example.com",
  "role": "USER",
  "birthDate": "1990-01-01",
  "gender": "male",
  "isPrivate": false,
  "profile": "https://zrr.kr/iPHf",
  "code": "A1B2C3D4"
}
```

**응답 본문 예시**
```json
{
  "status": "success",
  "message": "회원가입이 성공적으로 완료되었습니다.",
  "data": null
}
```

### 4.2. 로그인

*참고: `/api/auth/login`이 `user_management_api.md`의 사용자 관리 섹션에 나열되어 있지만, 핵심 인증 프로세스 및 토큰 발급을 다루므로 의미상 인증 API 섹션(섹션 5)에 더 적합합니다.* *`user_management_api.md`에는 `AuthController`가 처리하는 `/api/auth/login`도 나열되어 있습니다.*

### 4.3. 프로필 이미지 업로드

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/profileUpload`                                          |
| 설명        | 사용자 프로필 이미지를 업로드하고 이미지 URL을 반환받습니다.                         |
| 요청 본문   | multipart/form-data 형식<br>profile: 이미지 파일                        |
| 응답 코드   | 200: 업로드 성공<br>400: 잘못된 파일<br>500: 서버 오류                          |
| 응답 본문   | { "status": "success", "message": "프로필 이미지가 성공적으로 업로드되었습니다.", "data": { "fileName": "https://your-file-server.com/attach/profile/xxxx_profile.jpg" } } |

**요청 본문 예시**
```json
{
  "fileName": "https://your-file-server.com/attach/profile/xxxx_profile.jpg"
}
```

**응답 본문 예시**
```json
{
  "status": "success",
  "message": "프로필 이미지가 성공적으로 업로드되었습니다.",
  "data": {
    "fileName": "https://your-file-server.com/attach/profile/xxxx_profile.jpg"
  }
}
```

### 4.4. 사용자 ID 중복 체크

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/check/userId/IsDuplicate`                               |
| 설명        | 제공된 사용자 ID가 이미 사용 중인지 확인합니다.                                |
| 요청 본문   | { "userId": "newUser123" }                                           |
| 응답 코드   | 200: 중복 체크 결과                                                      |
| 응답 본문   | { "status": "success", "message": "사용자 ID 중복 확인이 완료되었습니다.", "data": false }                                 |

**요청 본문 예시**
```json
{
  "userId": "newUser123"
}
```

**응답 본문 예시**
```json
{
  "status": "success",
  "message": "사용자 ID 중복 확인이 완료되었습니다.",
  "data": false
}
```

### 4.5. 닉네임 중복 체크

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/check/nickname/IsDuplicate`                             |
| 설명        | 제공된 닉네임이 이미 사용 중인지 확인합니다.                                 |
| 요청 본문   | { "nickname": "쾌활한다람쥐" }                                       |
| 응답 코드   | 200: 중복 체크 결과                                                      |
| 응답 본문   | { "status": "success", "message": "닉네임 중복 확인이 완료되었습니다.", "data": false }                                 |

**요청 본문 예시**
```json
{
  "nickname": "쾌활한다람쥐"
}
```

**응답 본문 예시**
```json
{
  "status": "success",
  "message": "닉네임 중복 확인이 완료되었습니다.",
  "data": false
}
```

### 4.6. 사용자 토큰 쿠키 정리 (로그아웃)

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/clean/userTokenCookie`                                  |
| 설명        | 클라이언트의 refreshToken 쿠키를 만료시켜 제거합니다. (소프트 로그아웃)        |
| 응답 코드   | 200: 쿠키 정리 성공                                                     |
| 응답 본문   | { "status": "success", "message": "리프레시 토큰이 성공적으로 삭제되었습니다.", "data": null }                 |

**요청 본문 예시**
```json
{
  "status": "success",
  "message": "리프레시 토큰이 성공적으로 삭제되었습니다.",
  "data": null
}
```

---

## 6. 관리자 필터 관리 API

**컨트롤러**: `AdminFilterController.java`

**기본 경로**: `/admin/filters`

**권한 부여**: 이 섹션의 모든 엔드포인트에는 `ADMIN` 역할이 필요합니다.

이 API를 통해 관리자는 시스템 내에서 플러그형 필터를 관리하고 구성할 수 있습니다. 이러한 필터는 정의된 조건에 따라 요청을 가로챌 수 있으므로 보안 규칙이나 요청 처리 동작을 동적으로 수정할 수 있습니다.

### 6.1. 등록된 모든 필터 가져오기

| 항목        | 설명                                                                 |
| ----------- | --------------------------------------------------------------------------- |
| 메소드      | GET                                                                         |
| 엔드포인트  | `/admin/filters`                                                            |
| 설명        | 현재 등록된 모든 플러그형 필터 및 적용된 조건 목록을 검색합니다.            |
| 요청        | 없음                                                                        |
| 응답 코드   | 200: 성공<br>401: 권한 없음<br>403: 금지됨                                  |
| 응답 본문 (성공 200) | FilterListResponse (구조는 데이터 모델 참조) <br> 예시: <br> { "filters": [ { "filterId": "jwtVerificationFilter", "filterClassName": "JwtVerificationFilter", "conditions": [ { "id": "uuid1-jwt-public", "description": "public paths", "patterns": ["/public/**"], "methods": [] } ] } ] } |

**요청 본문 예시**
```json
{}
```

**응답 본문 예시**
```json
{
  "filters": [
    {
      "filterId": "jwtVerificationFilter",
      "filterClassName": "JwtVerificationFilter",
      "conditions": [
        {
          "id": "uuid1-jwt-public",
          "description": "public paths",
          "patterns": ["/public/**"],
          "methods": []
        }
      ]
    }
  ]
}
```

### 6.2. 필터에 조건 추가

| 항목        | 설명                                                                 |
| ----------- | --------------------------------------------------------------------------- |
| 메소드      | POST                                                                        |
| 엔드포인트  | `/admin/filters/{filterId}/conditions`                                      |
| 설명        | 지정된 필터에 새 `PathPatternFilterCondition`을 추가합니다. 이 조건은 필터 동작이 변경될 수 있는 (예: 건너뛰기) URL 패턴 및 HTTP 메소드를 정의합니다. |
| 경로 변수   | filterId (문자열): 조건을 추가할 필터의 고유 ID (예: "jwtVerificationFilter"). |
| 요청 본문   | AddConditionRequest (구조는 데이터 모델 참조) <br> 예시: <br> { "description": "Allow public access to specific API endpoints", "patterns": ["/api/public/version", "/api/public/health"], "methods": ["GET"] } |
| 응답 코드   | 200: 성공<br>400: 잘못된 요청 (예: 잘못된 요청 본문)<br>401: 권한 없음<br>403: 금지됨<br>404: 필터를 찾을 수 없음 |
| 응답 본문 (성공 200) | MessageResponse (구조는 데이터 모델 참조) <br> 예시: <br> { "message": "Condition added successfully to filter jwtVerificationFilter" } |

**요청 본문 예시**
```json
{
  "description": "Allow public access to specific API endpoints",
  "patterns": ["/api/public/version", "/api/public/health"],
  "methods": ["GET"]
}
```

**응답 본문 예시**
```json
{
  "message": "Condition added successfully to filter jwtVerificationFilter"
}
```

### 6.3. 필터에서 조건 제거

| 항목        | 설명                                                                 |
| ----------- | --------------------------------------------------------------------------- |
| 메소드      | DELETE                                                                      |
| 엔드포인트  | `/admin/filters/{filterId}/conditions/{conditionId}`                        |
| 설명        | 조건의 고유 ID를 사용하여 지정된 필터에서 특정 조건을 제거합니다.             |
| 경로 변수   | filterId (문자열): 필터 ID.<br>conditionId (문자열): 제거할 조건의 고유 ID. |
| 요청        | 없음                                                                        |
| 응답 코드   | 200: 성공<br>401: 권한 없음<br>403: 금지됨<br>404: 필터 또는 조건을 찾을 수 없음 |
| 응답 본문 (성공 200) | MessageResponse (구조는 데이터 모델 참조) <br> 예시: <br> { "message": "Condition uuid1-jwt-public removed successfully from filter jwtVerificationFilter" } |

**요청 본문 예시**
```json
{}
```

**응답 본문 예시**
```json
{
  "message": "Condition uuid1-jwt-public removed successfully from filter jwtVerificationFilter"
}
```

### 6.4. 필터 상태 설정 (활성화/비활성화)

| 항목        | 설명                                                                 |
| ----------- | --------------------------------------------------------------------------- |
| 메소드      | POST                                                                        |
| 엔드포인트  | `/admin/filters/{filterId}/status`                                          |
| 설명        | 특정 필터를 활성화하거나 비활성화합니다. <br> - 필터 **활성화**는 해당 로직에 따라 요청을 처리함을 의미합니다 (특정 조건으로 인해 건너뛰지 않는 한). 일반적으로 항상 `shouldNotFilter = false`로 평가되는 `FilterCondition`을 추가하여 수행됩니다. <br> - 필터 **비활성화**는 요청을 처리하지 않음을 의미합니다. 일반적으로 항상 `shouldNotFilter = true`로 평가되는 `FilterCondition`을 추가하여 수행됩니다. <br> 새 상태를 적용하기 전에 기존의 활성화/비활성화 관리 조건은 제거됩니다. |
| 경로 변수   | filterId (문자열): 필터 ID.                                  |
| 쿼리 매개변수 | action (문자열): 수행할 작업. "enable" 또는 "disable"이어야 합니다.       |
| 요청 본문   | 없음                                                                        |
| 응답 코드   | 200: 성공<br>400: 잘못된 요청 (예: 잘못된 작업 매개변수)<br>401: 권한 없음<br>403: 금지됨<br>404: 필터를 찾을 수 없음 |
| 응답 본문 (성공 200) | MessageResponse (구조는 데이터 모델 참조) <br> 예시 (활성화): <br> { "message": "Filter jwtVerificationFilter enabled" } <br> 예시 (비활성화): <br> { "message": "Filter jwtVerificationFilter disabled" } |

**요청 본문 예시**
```json
{}
```

**응답 본문 예시**
```json
{
  "message": "Filter jwtVerificationFilter enabled"
}
```

---

## 5. 인증 API

**컨트롤러**: `TokenController.java`, `Oauth2Controller.java` (참고: `AuthController.java`는 `/auth_check` 엔드포인트만 포함).

**기본 경로**: `/api/auth`, `/auth`, 또는 `/oauth2` (개별 엔드포인트에서 지정한 대로)

### 5.1. 로그인 (JWT 발급)

**컨트롤러**: `TokenController.java`
*참고: 현재 `TokenController.java`의 로그인 관련 로직은 실제 인증을 수행하지 않는 **플레이스홀더** 상태입니다. 실제 Spring Security `AuthenticationManager`를 사용한 인증 로직 구현이 필요합니다.*

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/auth/login`                                                    |
| 설명        | 사용자 ID와 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.                         |
| 요청 본문   | { "userId": "newUser123", "password": "password123!" }                 |
| 응답 코드   | 200: 로그인 성공 (현재 플레이스홀더 기준)<br>400: 잘못된 요청<br>401: 로그인 실패 (현재 플레이스홀더 기준)                          |
| 응답 헤더   | Authorization: Bearer {액세스 토큰}<br>Set-Cookie: refreshToken=xxxxxx; Path=/; Domain=your-cookie-domain.com; HttpOnly; Secure | // 참고: 도메인을 구성해야 합니다.
| 응답 본문   | { "access_token": "dummy.access.token" } |

**요청 본문 예시**
```json
{
  "userId": "newUser123",
  "password": "password123!"
}
```

**응답 본문 예시**
```json
{
  "access_token": "dummy.access.token"
}
```

### 5.2. 인증 상태 확인 (테스트용)

*출처: `auth_api.md` (`AuthController.java`에서 처리)*

| 항목        | 설명                                         |
| ----------- | -------------------------------------------- |
| 메소드      | GET                                          |
| 엔드포인트  | `/auth_check`                                |
| 설명        | 현재 요청이 유효한 인증 토큰을 가지고 있는지 확인합니다. | 
| 인증        | Bearer Token 필요                            |
| 응답 코드   | 200: 인증됨<br>401: 인증되지 않음             |

**요청 본문 예시**
```json
{}
```

**응답 본문 예시**
```json
200
```

### 5.3. JWT 토큰 재발급

*출처: `auth_api.md` (`TokenController.java`에서 처리)*

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/auth/api/protected/refresh`                                        |
| 설명        | 만료된 Access Token과 유효한 Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. |
| 요청 본문   | { "expiredToken": "string", "provider": "string" } // 예: "server", "google" |
| 응답 코드   | 200: 토큰 재발급 성공<br>401: Refresh Token이 없거나 유효하지 않음<br>406: 요청이 유효하지 않거나 Redis에 Refresh Token이 없음 |
| 응답 헤더   | Authorization: Bearer {새로운 액세스 토큰}                                   |
| 응답 본문   | { "access_token": "new_eyJhbGciOiJIUzUxMiJ9..." } |

**요청 본문 예시**
```json
{
  "expiredToken": "string",
  "provider": "string"
}
```

**응답 본문 예시**
```json
{
  "access_token": "new_eyJhbGciOiJIUzUxMiJ9..."
}
```

### 5.4. OAuth2 인증

*출처: `oauth2_api.md` (`Oauth2Controller.java`에서 처리)*

다음 엔드포인트는 OAuth2 인증 흐름을 관리합니다.

#### 5.4.1. OAuth2 로그인 URL 가져오기

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | GET                                                                  |
| 엔드포인트  | `/api/public/oauth2/login_url/{provider}`                            |
| 설명        | 지정된 OAuth2 제공업체(provider)의 로그인 페이지로 리디렉션할 URL을 가져옵니다. 사용자는 이 URL로 이동하여 인증을 시작합니다. |
| 경로 파라미터 | provider: 문자열. OAuth2 제공업체 이름 (예: "google", "kakao", "naver"). `AuthProvider.ProviderType` 에 정의된 값 중 하나여야 합니다. |
| 응답 코드   | 200: 성공<br>400: 잘못된 제공업체 이름                               |
| 응답 본문   | { "login_url": "https://accounts.google.com/o/oauth2/v2/auth?client_id=..." } |

**요청 본문 예시**
```json
{}
```

**응답 본문 예시**
```json
{
  "login_url": "https://accounts.google.com/o/oauth2/v2/auth?client_id=..."
}
```

#### 5.4.2. OAuth2 Kakao 로그인 콜백

**컨트롤러**: `Oauth2Controller.java`

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/oauth2/callback/kakao`                                             |
| 설명        | Kakao OAuth2 인증 후, 코드를 받아 서버에서 로그인/회원가입 처리 및 JWT를 발급합니다. |
| 요청 본문   | { "tempCode": "authorization_code_from_kakao" } |
| 응답 코드   | 200: OAuth2 로그인/회원가입 성공, JWT 발급<br>400: 잘못된 요청 또는 코드<br>500: 서버 오류 |
| 응답 헤더   | Authorization: Bearer {액세스 토큰}<br>Set-Cookie: refreshToken=... (similar to standard login) |
| 응답 본문   | { "access_token": "eyJhbGciOiJIUzUxMiJ9...", "userProfile": { /* 사용자 프로필 정보 */ } } |

**요청 본문 예시**
```json
{
  "tempCode": "authorization_code_from_kakao"
}
```

**응답 본문 예시**
```json
{
  "access_token": "eyJhbGciOiJIUzUxMiJ9...",
  "userProfile": { /* 사용자 프로필 정보 */ }
}
```

#### 5.4.3. OAuth2 Naver 로그인 콜백

**컨트롤러**: `Oauth2Controller.java`

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/oauth2/callback/naver`                                             |
| 설명        | Naver OAuth2 인증 후, 코드를 받아 서버에서 로그인/회원가입 처리 및 JWT를 발급합니다. |
| 요청 본문   | { "tempCode": "authorization_code_from_naver", "state": "csrf_token_from_naver" } // Naver requires state parameter |
| 응답 코드   | 200: OAuth2 로그인/회원가입 성공, JWT 발급<br>400: 잘못된 요청, 코드 또는 state 불일치<br>500: 서버 오류 |
| 응답 헤더   | Authorization: Bearer {액세스 토큰}<br>Set-Cookie: refreshToken=... |
| 응답 본문   | { "access_token": "eyJhbGciOiJIUzUxMiJ9...", "userProfile": { /* 사용자 프로필 정보 */ } } |

**요청 본문 예시**
```json
{
  "tempCode": "authorization_code_from_naver",
  "state": "csrf_token_from_naver"
}
```

**응답 본문 예시**
```json
{
  "access_token": "eyJhbGciOiJIUzUxMiJ9...",
  "userProfile": { /* 사용자 프로필 정보 */ }
}
```

#### 5.4.4. OAuth2 Google 로그인 콜백

**컨트롤러**: `Oauth2Controller.java`

| 항목        | 설명                                                                 |
| ----------- | -------------------------------------------------------------------- |
| 메소드      | POST                                                                 |
| 엔드포인트  | `/api/public/oauth2/callback/google`                                            |
| 설명        | Google OAuth2 인증 후, 코드를 받아 서버에서 로그인/회원가입 처리 및 JWT를 발급합니다. |
| 요청 본문   | { "tempCode": "authorization_code_from_google" } |
| 응답 코드   | 200: OAuth2 로그인/회원가입 성공, JWT 발급<br>400: 잘못된 요청 또는 코드<br>500: 서버 오류 |
| 응답 헤더   | Authorization: Bearer {액세스 토큰}<br>Set-Cookie: refreshToken=... |
| 응답 본문   | { "access_token": "eyJhbGciOiJIUzUxMiJ9...", "userProfile": { /* 사용자 프로필 정보 */ } } |

**요청 본문 예시**
```json
{
  "tempCode": "authorization_code_from_google"
}
```

**응답 본문 예시**
```json
{
  "access_token": "eyJhbGciOiJIUzUxMiJ9...",
  "userProfile": { /* 사용자 프로필 정보 */ }
}
```

---

## 7. 데이터 모델

This section outlines the primary data models used for requests and responses across the various APIs. For detailed field descriptions, refer to the corresponding Java DTO classes within the project.

*Implementation Guideline: All data model Repository implementations should use JPA (Java Persistence API) and QueryDSL (specifically, the `io.github.openfeign.querydsl` fork). Direct SQL query writing is discouraged to maintain type safety, readability, and maintainability.*

### 7.1. Authentication and User Models

| Model                 | Description        | Key Fields                                                                 |
| --------------------- | ------------------ | -------------------------------------------------------------------------- |
| `JoinRequest`         | 회원가입 요청      | `userId`, `userPw`, `userName`, `nickname`, `phone`, `email`, `role`, `birthDate`, `gender`, `isPrivate`, `profile`, `code` |
| `LoginRequest`        | 로그인 요청        | `userId`, `password`                                                       |
| `LoginResponse`       | 로그인 응답        | `access_token`                                                             |
| `TokenRefreshRequest` | 토큰 갱신 요청     | `expiredToken`, `provider`                                                 |
| `TokenRefreshResponse`| 토큰 갱신 응답     | `access_token`                                                             |
| `OAuth2CallbackRequest`| OAuth2 콜백 요청  | `tempCode`, `state`                                                        |
| `OAuth2LoginUrlResponse`| OAuth2 로그인 URL 응답 | `login_url`                                                              |
| `OAuth2LoginResponse` | OAuth2 로그인 응답 | `access_token`, `userProfile`                                              |

### 7.2. Email Models

| Model                 | Description        | Key Fields                |
| --------------------- | ------------------ | ------------------------- |
| `EmailRequest`        | 이메일 요청        | `email`                   |
| `EmailSendResponse`   | 이메일 전송 응답   | `message`                 |
| `CustomEmailRequest`  | 커스텀 이메일 요청 | `email`, `content`, `title` |
| `EmailCheckDto`       | 이메일 확인 요청   | `email`, `code`           |
| `EmailCheckResponse`  | 이메일 확인 응답   | `message`                 |

### 7.3. Diary and Analysis Models

| Model                          | Description        | Key Fields                                                               |
| ------------------------------ | ------------------ | ------------------------------------------------------------------------ |
| `DiaryCreateRequest`           | 일기 생성 요청     | `title`, `content`                                                       |
| `DiaryResponse`                | 일기 응답          | `id`, `userId`, `title`, `content`, `createdAt`, `updatedAt`             |
| `DiaryUpdateRequest`           | 일기 수정 요청     | `title`, `content`                                                       |
| `DiaryListItem`                | 일기 목록 항목     | `id`, `title`, `createdAt`, `emotionStatus`                              |
| `DiaryDetailResponse`          | 일기 상세 응답     | `id`, `userId`, `title`, `content`, `alternativeThoughtByAI`, `createdAt`, `updatedAt`, `analysis` |
| `DiaryAnalysisResult`          | 일기 분석 결과     | `id`, `emotionDetection`, `automaticThought`, `promptForChange`, `alternativeThought`, `status`, `analyzedAt` |
| `DiaryAnalysisRequestResponse` | 일기 분석 요청 응답 | `message`, `diaryId`, `trackingId`                                       |

### 7.4. Settings Models

| Model                       | Description          | Key Fields                                                        |
| --------------------------- | -------------------- | ----------------------------------------------------------------- |
| `SettingItem`               | 설정 항목            | `settingKey`, `value`, `dataType`, `description`, `isUserEditable` |
| `SettingsListResponse`      | 설정 목록 응답       | `settings`                                                        |
| `SettingsUpdateRequestItem` | 설정 수정 요청 항목  | `settingKey`, `newValue`                                          |
| `SettingsUpdateRequest`     | 설정 수정 요청       | `settingsToUpdate`                                                |
| `SettingsUpdateResponse`    | 설정 수정 응답       | `message`, `updatedSettings`                                      |

### 7.5. Error Models

| Model           | Description | Key Fields                                     |
| --------------- | ----------- | ---------------------------------------------- |
| `ErrorResponse` | 오류 응답   | `timestamp`, `status`, `error`, `message`, `path` |

### 7.6. Admin Filter Models

These models are used by the Admin Filter Management API.

| Model                 | Description                           | Key Fields                                                                 |
| --------------------- | ------------------------------------- | -------------------------------------------------------------------------- |
| `FilterInfo`          | Information about a registered filter | `filterId`, `filterClassName`, `conditions` (list of `ConditionInfo`)    |
| `ConditionInfo`       | Information about a filter condition  | `id`, `description`, `patterns` (Set of String), `methods` (Set of String) |
| `FilterListResponse`  | Response for listing all filters      | `filters` (list of `FilterInfo`)                                           |
| `AddConditionRequest` | Request to add a condition to a filter| `description` (String), `patterns` (Set of String), `methods` (Set of HttpMethod) |
| `MessageResponse`     | Generic message response              | `message` (String)                                                         |

### 7.7 공통 응답 모델 (Common Response Models)

| Model                 | Description        | Key Fields                                      |
| --------------------- | ------------------ | ----------------------------------------------- |
| `ApiResponse<T>`      | 공통 API 응답 구조 | `status` (String), `message` (String), `data` (T) |

---
