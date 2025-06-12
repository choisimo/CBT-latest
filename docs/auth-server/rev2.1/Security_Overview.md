# 보안 개요

이 문서는 Auth-Server Spring Security 애플리케이션의 보안 아키텍처를 개요로 설명하며, 인증, 인가, 그리고 JWT(JSON Web Token)의 사용에 중점을 둡니다.

## 핵심 원칙

*   **JWT 기반 인증**: 사용자는 한 번 인증을 받고 JWT를 발급받아 이후 요청에 사용합니다.
*   **필터 기반 보안**: 커스텀 Spring Security 필터가 인증 및 인가 로직을 처리합니다.
*   **역할 기반 인가**: 특정 API 엔드포인트 접근은 사용자 역할(예: `ROLE_ADMIN`)에 따라 제한됩니다.
*   **CORS 설정**: 다양한 오리진에서의 안전한 통신을 위해 적절한 CORS(Cross-Origin Resource Sharing) 설정이 적용되어 있습니다.

## 주요 구성요소

### `SecurityFilterConfig`

*   **목적**: Spring Security 필터 체인을 구성하고 커스텀 필터를 등록합니다.
*   **위치**: `com.authentication.auth.config.SecurityFilterConfig.java`
*   **기능**:
    *   `AuthenticationFilter`와 `AuthorizationFilter`를 Spring Security 필터 체인에 등록합니다.
    *   이 커스텀 필터들이 올바른 순서로 실행되도록 보장합니다.

### `AuthenticationFilter`

*   **목적**: 주로 로그인 요청을 처리하고 JWT를 생성하여 사용자 인증을 담당합니다.
*   **위치**: `com.authentication.auth.filter.AuthenticationFilter.java`
*   **기능**:
    *   로그인 요청(예: `/api/auth/login`)을 가로챕니다.
    *   사용자 자격 증명을 검증합니다.
    *   인증에 성공하면 Access Token과 Refresh Token을 생성합니다.
    *   Access Token은 `Authorization` 헤더에, Refresh Token은 `HttpOnly` 쿠키로 추가합니다.

### `AuthorizationFilter`

*   **목적**: JWT를 검증하고, 사용자 역할 및 요청 경로에 따라 접근 제어를 수행합니다.
*   **위치**: `com.authentication.auth.filter.AuthorizationFilter.java`
*   **기능**:
    *   보호된 리소스에 대한 모든 요청을 가로챕니다.
    *   `Authorization` 헤더에서 JWT를 추출 및 검증합니다.
    *   JWT에서 사용자 역할을 파싱합니다.
    *   인증된 사용자가 요청 경로에 접근할 권한이 있는지 확인합니다.
    *   특히, 경로가 `SecurityConstants.ADMIN_API_PATH`로 시작하면 `ROLE_ADMIN` 또는 `ADMIN` 역할을 확인합니다.
    *   `SecurityConstants.PUBLIC_PATHS`에 정의된 공개 경로는 인가 검증을 건너뜁니다.
    *   토큰 갱신 요청도 처리합니다.

### `SecurityConstants`

*   **목적**: JWT 비밀키, 토큰 만료 시간, 다양한 API 경로 패턴 등 보안 관련 상수를 정의합니다.
*   **위치**: `com.authentication.auth.others.constants.SecurityConstants.java`
*   **주요 상수**:
    *   `JWT_SECRET`
    *   `ACCESS_TOKEN_EXPIRATION_TIME`
    *   `REFRESH_TOKEN_EXPIRATION_TIME`
    *   `PUBLIC_API_PATH`: 공개 엔드포인트의 기본 경로(예: `/api/public/**`)
    *   `ADMIN_API_PATH`: 관리자 전용 엔드포인트의 기본 경로(예: `/api/admin/**`)
    *   `/api/auth/login`, `/api/auth/join`, `/api/public/emailSend`, `/swagger-ui.html` 등과 같은 특정 공개 경로

### `WebConfig` (CORS 설정)

*   **목적**: 다양한 오리진에서 웹 브라우저가 API에 요청할 수 있도록 CORS를 설정합니다.
*   **위치**: `com.authentication.auth.config.WebConfig.java`
*   **기능**:
    *   지정된 오리진(예: `http://localhost:8080`, `http://localhost:3000`, `http://localhost:7078`, `http://localhost:7077`, `https://oss-emotion.nodove.com`)에서의 요청을 허용합니다.
    *   일반적인 HTTP 메서드(`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`)를 허용합니다.
    *   모든 헤더를 허용합니다.
    *   클라이언트에서 `Authorization` 헤더를 사용할 수 있도록 노출합니다.
    *   자격 증명(쿠키, HTTP 인증 등)을 허용합니다.
    *   pre-flight 요청의 최대 유효 시간을 설정합니다.

## 인증 플로우

1.  **사용자 로그인**: 사용자가 자격 증명을 담아 `POST /api/auth/login` 요청을 보냅니다.
2.  **AuthenticationFilter**: 요청을 가로채어 자격 증명을 검증하고, 성공 시 JWT Access Token과 Refresh Token을 생성합니다.
3.  **토큰 발급**: Access Token은 `Authorization` 헤더로, Refresh Token은 `HttpOnly` 쿠키로 반환됩니다.
4.  **이후 요청**: 보호된 리소스 접근 시, 클라이언트는 Access Token을 `Authorization: Bearer <token>` 헤더에 포함시켜 요청합니다.
5.  **AuthorizationFilter**: 요청을 가로채어 Access Token을 검증하고, 사용자 역할을 추출하여 접근 권한을 확인합니다.

## 인가 플로우

1.  **요청 가로채기**: `AuthorizationFilter`가 모든 요청을 가로챕니다.
2.  **공개 경로 확인**: 요청 경로가 `SecurityConstants`에 정의된 공개 경로라면 추가 인가 검증 없이 통과시킵니다.
3.  **토큰 검증**: 보호된 경로의 경우 JWT를 검증합니다. 토큰이 없거나 유효하지 않으면 인증 실패 응답을 반환합니다.
4.  **역할 기반 접근 제어**: 토큰이 유효하면 JWT에서 추출한 역할이 해당 리소스 접근에 적합한지 확인합니다. `AuthorizationFilter`가 이 역할 검증을 담당합니다.
    *   **`ROLE_USER`:** 이 역할을 가진 사용자는 자신의 데이터 및 일반 기능에 접근할 수 있습니다.
        *   **접근 가능한 엔드포인트 예시:**
            *   `GET /api/diaries`, `POST /api/diaries/{diaryId}`: 자신의 일기 관리
            *   `GET /api/settings`, `PUT /api/settings`: 개인 설정 조회 및 수정
            *   `POST /api/protected/sendEmailPassword`: 로그인된 상태에서 비밀번호 변경 요청
            *   SSE 구독: `GET /api/protected/sse/subscribe`
        *   관리자 전용 엔드포인트에는 접근할 수 없습니다.
    *   **`ROLE_ADMIN`:** 이 역할을 가진 사용자는 시스템, 사용자 관리 등 모든 데이터에 접근할 수 있습니다.
        *   **접근 가능한 엔드포인트 예시:**
            *   `/api/admin/**` 하위 모든 엔드포인트(`SecurityConstants.ADMIN_API_PATH`에 정의됨)
            *   동적 시스템 필터 관리를 위한 `AdminFilterController` 엔드포인트(예: `GET /admin/filters`, `POST /admin/filters/{filterId}/conditions`)
            *   시스템 로그, 메트릭 조회, 기타 관리 작업용 엔드포인트 등
        *   일반 사용자 기능도 모두 접근 가능합니다.
    *   역할 구분은 `AuthorizationFilter`가 JWT에서 추출한 역할과 요청 경로의 요구사항을 비교하여 강제합니다. 현재 구성에서는 이 두 가지 주요 역할 외에 복잡한 계층 구조는 없으나, 필터 로직을 커스터마이징하거나 더 세분화된 권한을 추가하여 확장할 수 있습니다.
5.  **접근 결정**: 토큰 유효성 및 역할 검증 결과에 따라 컨트롤러로 요청을 전달하거나, 적절한 HTTP 상태 코드(`401 Unauthorized`, `403 Forbidden` 등)로 거부합니다.

## 사용자 계정 관리 보안

이 섹션은 사용자 계정 인증 및 복구 프로세스의 보안 측면을 다룹니다.

### 이메일 인증 플로우

사용자의 이메일이 실제로 본인 소유임을 검증하는 것은 계정 보안 및 커뮤니케이션에 매우 중요합니다.

*   **시작:** 신규 가입 또는 기존 사용자가 이메일을 인증/변경하려 할 때 이메일 인증 프로세스가 시작됩니다. `POST /api/public/emailSend`(API_Documentation.md 참고)와 같은 API 호출로 인증할 이메일을 전달합니다.
*   **토큰 생성 및 전달:**
    *   요청을 받으면 백엔드는 고유한 인증 토큰(또는 코드)을 생성합니다. 생성 방식(랜덤 문자열, JWT 등)은 구현에 따라 다르지만 충분히 예측 불가능해야 합니다.
    *   이 토큰은 이메일, 만료 시간과 함께 임시 저장소(예: Redis, DB 테이블)에 저장됩니다.
    *   인증 토큰(또는 토큰이 포함된 링크)이 사용자의 이메일로 전송됩니다.
*   **토큰 제출 및 검증:**
    *   사용자는 이메일에서 토큰을 확인한 뒤, `POST /api/public/emailCheck`(API_Documentation.md 참고)와 같은 API로 제출합니다.
    *   백엔드는 제출된 토큰이 저장된 것과 일치하는지, 만료되지 않았는지, 이메일과 매칭되는지 검증합니다.
*   **결과:**
    *   **성공:** 토큰이 유효하면 해당 이메일이 인증됨으로 표시됩니다. 신규 가입의 경우 DB의 계정 상태가 "PENDING_VERIFICATION"에서 "ACTIVE"로 변경될 수 있습니다. 인증 토큰은 즉시 무효화/삭제됩니다.
    *   **실패:** 토큰이 유효하지 않거나 만료, 불일치 시 오류가 반환됩니다.
*   **보안 고려사항:**
    *   **토큰 만료:** 인증 토큰은 10~30분 등 짧은 유효기간을 가져야 하며, 이메일 계정이 탈취된 경우 악용 위험을 줄입니다.
    *   **일회성 사용:** 토큰은 한 번만 사용 가능해야 하며, 인증 후 즉시 무효화해야 합니다.
    *   **요청 제한:** 인증 메일 발송/검증 API는 남용 방지를 위해 rate limit이 필요합니다(이메일 폭탄, 토큰 brute-force 등 방지).

### 비밀번호 재설정 플로우

비밀번호를 잊은 사용자를 위한 안전한 재설정 플로우가 필요합니다.

*   **시작:**
    *   사용자는 로그인 페이지에서 "비밀번호 찾기"를 클릭합니다.
    *   `GET /api/public/findPassWithEmail`(API_Documentation.md 참고)과 같은 엔드포인트로 가입된 아이디(또는 이메일)를 제출합니다.
*   **토큰 생성 및 전달:**
    *   사용자가 확인되면, 백엔드는 고유하고 암호학적으로 강력하며, 시간 제한이 있는 비밀번호 재설정 토큰을 생성합니다.
    *   이 토큰은 사용자 계정과 연결되어 안전하게 저장됩니다(DB에 해시 저장 또는 안전한 캐시에 저장 등).
    *   토큰이 포함된 고유 링크가 사용자의 이메일로 전송됩니다.
*   **토큰 사용 및 비밀번호 변경:**
    *   사용자는 이메일의 링크를 클릭해 애플리케이션의 비밀번호 재설정 페이지로 이동합니다.
    *   애플리케이션은 URL에서 토큰을 추출합니다.
    *   사용자는 새 비밀번호를 입력해 제출합니다.
    *   클라이언트는 새 비밀번호와 재설정 토큰을 백엔드로 전송합니다(예: `POST /api/public/resetPassword`와 같은 엔드포인트, 실제로는 `UsersController.updatePassword`가 로그인 사용자를 위한 것일 수 있으니 확인 필요). 내부적으로 `UserService.UpdateUserPassword(userId, temporalPassword)`가 사용될 수 있으나, 일반적으로는 토큰과 새 비밀번호를 함께 받는 별도 엔드포인트가 필요합니다.
*   **결과:**
    *   **성공:** 토큰이 유효(존재, 사용자와 일치, 만료되지 않음)하고 새 비밀번호가 정책을 만족하면, 백엔드는 DB에 새 비밀번호 해시를 저장합니다. 토큰은 즉시 무효화됩니다. 사용자는 비밀번호 변경 성공 알림을 받고, 새 비밀번호로 로그인할 수 있습니다.
    *   **실패:** 토큰이 유효하지 않거나 만료, 기타 오류 시 에러 메시지가 표시됩니다.
*   **보안 고려사항:**
    *   **토큰 보안:** 토큰은 길고, 랜덤하며, 예측 불가능해야 합니다. 민감한 자격 증명처럼 다뤄야 합니다.
    *   **토큰 만료:** 토큰은 1~2시간 등 짧은 유효기간을 가져야 합니다.
    *   **일회성 사용:** 토큰은 한 번 사용 후 즉시 무효화해야 합니다.
    *   **안전한 전송:** 재설정 링크와 비밀번호 입력 페이지는 반드시 HTTPS로 제공되어야 합니다.
    *   **알림:** 비밀번호 재설정이 시작되거나 완료될 때 사용자에게 이메일로 알림을 보내야 합니다(비인가 시도 탐지 목적).
    *   **요청 제한:** 토큰 제출 및 재설정 시작 엔드포인트에 brute-force 방지 rate limit이 필요합니다.

## 토큰 갱신 메커니즘

Access Token이 만료되면, 클라이언트는 만료된 Access Token과 Refresh Token(쿠키로 전송됨)을 포함해 `/auth/api/protected/refresh`로 `POST` 요청을 보낼 수 있습니다. `AuthorizationFilter`가 이 요청을 처리하여 Refresh Token을 검증하고, 새로운 Access Token을 발급합니다.
