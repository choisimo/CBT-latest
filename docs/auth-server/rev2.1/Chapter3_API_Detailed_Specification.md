# Chapter 3: API Detailed Specification

## 3.1. API Design Principles and Version Management Strategy

*   **Design Principles:**
    *   **RESTful:** APIs are designed following REST principles, utilizing standard HTTP methods (GET, POST, PUT, DELETE), URI-based resource identification, and stateless communication.
    *   **Stateless:** Each request from a client to the server must contain all the information needed to understand and process the request. The server does not store any client context between requests. Session management for authenticated users is handled via JWTs.
    *   **JSON for Data Exchange:** Request and response bodies primarily use `application/json` as the content type.
    *   **Consistent URL Structure:** URLs are designed to be intuitive and hierarchical.
    *   **Standard HTTP Status Codes:** Standard HTTP status codes are used to indicate the outcome of API requests.
*   **Version Management Strategy:**
    *   **URI Versioning:** The API version is included in the URI path (e.g., `/api/v1/...`). Currently, most public APIs are prefixed with `/api/public/` or `/api/protected/` and specific versioning (e.g., v1) is not explicitly shown in the existing `API_Documentation.md` paths but is a recommended practice for future evolution. For this document, we will assume future versioning will adopt `/api/v1/...` if not already implicitly handled by base paths.

## 3.2. API 목록 및 그룹핑

The APIs are grouped logically as follows:

1.  **Authentication API:** Handles user login and token management.
2.  **User Management API:** Manages user accounts, registration, and profile-related information.
3.  **OAuth2 API:** Manages authentication via third-party OAuth2 providers.
4.  **Email API:** Handles sending and verifying emails (e.g., for verification codes, password resets).
5.  **SSE (Server-Sent Events) API:** Manages real-time communication from server to client.
6.  **Diary API (New and Existing):** Manages diary entries (CRUD operations).
7.  **Analysis API (Related to Diary):** (Implicitly part of Diary API, refers to AI analysis results associated with diary entries).
8.  **Settings API (New and Existing):** Manages user-specific and application settings.

## 3.3. 각 API별 상세 정의

(Note: For " 연관 컴포넌트 추적 (Traceability)", if specific controller/service methods are not found for new APIs, this will be noted, and traceability will focus on DTOs and Entities.)

---

### 3.3.A. Authentication API

#### 1. User Login (JWT Issuance)

*   **3.3.1.1. 요청 (Request)**
    *   **HTTP Method:** `POST`
    *   **Endpoint Path:** `/api/auth/login`
    *   **Full URL 예시:** `https://<your-domain>/api/auth/login`
    *   **기능 설명:** Authenticates a user with their ID and password, and if successful, issues JWT access and refresh tokens.
    *   **Path Parameters:** None.
    *   **Query Parameters:** None.
    *   **Header Parameters:** None.
    *   **Request Body:**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `LoginRequest.java` (`com.authentication.auth.dto.users.LoginRequest`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명   | 데이터 타입 | 제약조건         | 설명             | 예시           | 필수 |
            |----------|-------------|------------------|------------------|----------------|------|
            | `userId` | String      | NotBlank, 4-20자 | 사용자 로그인 ID | "testuser123"  | Yes  |
            | `password`| String      | NotBlank, min 8자| 사용자 비밀번호  | "P@sswOrd!"    | Yes  |

*   **3.3.1.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Login successful.
        *   `400 Bad Request`: Invalid request format (e.g., missing fields).
        *   `401 Unauthorized`: Login failed (invalid credentials).
        *   `500 Internal Server Error`: Server-side error during login.
    *   **Response Headers:**
        *   `Authorization`: `Bearer {access_token}` - Contains the issued JWT access token.
        *   `Set-Cookie`: `refreshToken={refresh_token}; Path=/; HttpOnly; Secure; Max-Age={expiry}` - Contains the JWT refresh token.
        *   `Set-Cookie`: `accessToken={access_token}; Path=/; HttpOnly; Secure; Max-Age={expiry}` - Also sets access token as cookie.
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `TokenDto.java` (`com.authentication.auth.dto.token.TokenDto`) - API Doc shows `LoginResponse` with only `access_token`, but `AuthenticationFilter` uses `TokenDto`.
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명         | 데이터 타입 | 설명                     | 예시                                                                 |
            |---------------|-------------|--------------------------|----------------------------------------------------------------------|
            | `accessToken` | String      | 발급된 JWT 액세스 토큰   | "eyJhbGciOiJIUzUxMiJ9..."                                            |
            | `refreshToken`| String      | 발급된 JWT 리프레시 토큰 | "eyJhbGciOiJIUzUxMiJ9..."                                            |

*   **3.3.1.3. 인증 및 인가:**
    *   인증 필요 여부: No (This is the authentication entry point).
    *   필요한 권한: None.

*   **3.3.1.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `AuthenticationFilter.java` (acts as a processing filter for `/api/auth/login`)
    *   **Service:** `AuthenticationManager` (Spring Security), `TokenProvider.java` (`com.authentication.auth.service.token.TokenProvider`), `RedisService.java` (`com.authentication.auth.service.redis.RedisService`)
    *   **Domain/Entity:** `User.java` (`com.authentication.auth.domain.User`)
    *   **DTO:** `LoginRequest.java`, `TokenDto.java`
    *   **Repository:** `UserRepository.java` (via `PrincipalDetailService`)

*   **3.3.1.5. 예외 처리 (Error Handling):**
    *   `401 Unauthorized`: Invalid credentials. Response body: `{"status": "error", "message": "Authentication failed", ...}` (standard `ErrorResponse` structure).
    *   `400 Bad Request`: Malformed request.

*   **3.3.1.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X POST \
          https://<your-domain>/api/auth/login \
          -H 'Content-Type: application/json' \
          -d '{
                "userId": "testuser123",
                "password": "P@sswOrd!"
              }'
        ```
    *   **Response Body (200 OK):**
        ```json
        {
          "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEyMyIsInJvbGUiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTcxNzE0MDQwMCwiZXhwIjoxNzE3MTQyMjAwfQ.exampleAccessToken",
          "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJpYXQiOjE3MTcxNDA0MDAsImV4cCI6MTcxOTczMjQwMH0.exampleRefreshToken"
        }
        ```

#### 2. JWT Token Re-issuance

*   **3.3.2.1. 요청 (Request)**
    *   **HTTP Method:** `POST`
    *   **Endpoint Path:** `/auth/api/protected/refresh` (Path from `TokenController.java`, `API_Documentation.md` also shows `/auth/api/protected/refresh`)
    *   **Full URL 예시:** `https://<your-domain>/auth/api/protected/refresh`
    *   **기능 설명:** Re-issues a new JWT access token using a valid refresh token.
    *   **Path Parameters:** None.
    *   **Query Parameters:** None.
    *   **Header Parameters:**
        *   `Cookie`: Contains `refreshToken=your_refresh_token` (or `provider_refreshToken` if applicable)
    *   **Request Body:**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `TokenRefreshRequest.java` (`com.authentication.auth.dto.token.TokenRefreshRequest`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명         | 데이터 타입 | 제약조건 | 설명                                     | 예시                                   | 필수 |
            |---------------|-------------|----------|------------------------------------------|----------------------------------------|------|
            | `expiredToken`| String      | Required | 만료된 (또는 현재) Access Token        | "eyJhbGciOiJIUzUxMiJ9..."            | Yes  |
            | `provider`    | String      | Required | 토큰 발급자 (e.g., "server", "google") | "server"                               | Yes  |

*   **3.3.2.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Token re-issuance successful.
        *   `401 Unauthorized`: Invalid or expired refresh token, or provider mismatch.
        *   `406 Not Acceptable`: Invalid request payload or refresh token not found in Redis.
    *   **Response Headers:**
        *   `Authorization`: `Bearer {new_access_token}` - Contains the newly issued JWT access token.
        *   Potentially `Set-Cookie` if refresh token is also rotated (current implementation seems to reuse valid refresh tokens).
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `TokenRefreshResponse.java` (`com.authentication.auth.dto.token.TokenRefreshResponse`), which wraps `TokenDto` or just `access_token`. `API_Documentation.md` and DTO show only `access_token`.
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명         | 데이터 타입 | 설명                     | 예시                               |
            |---------------|-------------|--------------------------|------------------------------------|
            | `access_token`| String      | 새로 발급된 JWT 액세스 토큰 | "new_eyJhbGciOiJIUzUxMiJ9..."      |

*   **3.3.2.3. 인증 및 인가:**
    *   인증 필요 여부: Requires a valid Refresh Token (typically passed via HttpOnly cookie and/or request body if applicable for the provider). The `expiredToken` in the body is also part of the request.
    *   필요한 권한: None specific, tied to the validity of the refresh token.

*   **3.3.2.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `TokenController.java` (`com.authentication.auth.controller.auth.TokenController.refreshToken()`)
    *   **Service:** `TokenService.java` (`com.authentication.auth.service.token.TokenService.refreshToken()`), `JwtUtility.java`, `RedisService.java`
    *   **Domain/Entity:** `User.java` (indirectly, for claims)
    *   **DTO:** `TokenRefreshRequest.java`, `TokenRefreshResponse.java`
    *   **Repository:** `UserRepository.java` (indirectly), Redis operations.

*   **3.3.2.5. 예외 처리 (Error Handling):**
    *   `401 Unauthorized`: Refresh token invalid or expired.
    *   `406 Not Acceptable`: Refresh token not found in Redis or request malformed.

*   **3.3.2.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):** (Assuming refresh token is in cookie)
        ```bash
        curl -X POST \
          https://<your-domain>/auth/api/protected/refresh \
          -H 'Content-Type: application/json' \
          --cookie "refreshToken=your_actual_refresh_token" \
          -d '{
                "expiredToken": "your_expired_access_token",
                "provider": "server"
              }'
        ```
    *   **Response Body (200 OK):**
        ```json
        {
          "access_token": "new_eyJhbGciOiJIUzUxMiJ9.exampleNewAccessToken"
        }
        ```

---
### 3.3.B. User Management API

#### 1. User Registration (Join)

*   **3.3.1.1. 요청 (Request)**
    *   **HTTP Method:** `POST`
    *   **Endpoint Path:** `/api/public/join` (Path from `UsersController.java`, `API_Documentation.md` uses `/api/public/join`)
    *   **Full URL 예시:** `https://<your-domain>/api/public/join`
    *   **기능 설명:** Registers a new user in the system. Requires prior email verification (code).
    *   **Path Parameters:** None.
    *   **Query Parameters:** None.
    *   **Header Parameters:** None.
    *   **Request Body:**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `JoinRequest.java` (`com.authentication.auth.dto.users.JoinRequest`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명      | 데이터 타입 | 제약조건                                   | 설명                       | 예시                     | 필수 |
            |-------------|-------------|--------------------------------------------|----------------------------|--------------------------|------|
            | `userId`    | String      | NotBlank, Size(4-20)                       | 사용자 로그인 ID           | "newuser123"             | Yes  |
            | `userPw`    | String      | NotBlank, Size(min=8)                      | 사용자 비밀번호            | "P@sswOrd123!"           | Yes  |
            | `phone`     | String      | NotBlank, Pattern(^\\d{3}-\\d{3,4}-\\d{4}$) | 사용자 전화번호            | "010-1234-5678"          | Yes  |
            | `email`     | String      | Email                                      | 사용자 이메일 주소         | "user@example.com"       | Yes  |
            | `role`      | String      |                                            | 사용자 역할 (def: "USER")  | "USER"                   | No   |
            | `birthDate` | Date        |                                            | 사용자 생년월일 (YYYY-MM-DD) | "1990-01-01"             | No   |
            | `gender`    | String      | NotBlank                                   | 사용자 성별                | "male"                   | Yes  |
            | `isPrivate` | boolean     |                                            | 계정 공개 여부 (def: false)| false                    | No   |
            | `profile`   | String      |                                            | 프로필 이미지 URL          | "https://zrr.kr/iPHf"    | No   |
            | `code`      | String      | NotBlank                                   | 이메일 인증 코드           | "A1B2C3D4"               | Yes  |

*   **3.3.1.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Join successful (Note: `UsersController` returns 200 OK on success, `API_Documentation.md` states 200).
        *   `400 Bad Request`: Invalid request data (e.g., validation errors).
        *   `409 Conflict`: User ID or nickname already exists.
        *   `500 Internal Server Error`: Server-side error during registration.
    *   **Response Headers:** None specific.
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json` (Though controller returns simple string body)
        *   **연결된 DTO 클래스명:** None (Simple message like `{"message": "join successfully"}`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명    | 데이터 타입 | 설명           | 예시                  |
            |-----------|-------------|----------------|-----------------------|
            | `message` | String      | 성공/실패 메시지 | "join successfully" |

*   **3.3.1.3. 인증 및 인가:**
    *   인증 필요 여부: No.
    *   필요한 권한: None.

*   **3.3.1.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `UsersController.java` (`com.authentication.auth.controller.UsersController.join()`)
    *   **Service:** `UserService.java` (`com.authentication.auth.service.users.UserService.join()`), `UserEventProducer.java`
    *   **Domain/Entity:** `User.java`
    *   **DTO:** `JoinRequest.java`
    *   **Repository:** `UserRepository.java`

*   **3.3.1.5. 예외 처리 (Error Handling):**
    *   `409 Conflict`: `{"message": "already exist userId or nickname"}` or similar.
    *   `400 Bad Request`: If email code validation (assumed to be part of service logic) fails.
    *   Standard `ErrorResponse` for other errors.

*   **3.3.1.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X POST \
          https://<your-domain>/api/public/join \
          -H 'Content-Type: application/json' \
          -d '{
                "userId": "newuser123",
                "userPw": "P@sswOrd123!",
                "phone": "010-1234-5678",
                "email": "user@example.com",
                "gender": "male",
                "code": "A1B2C3D4",
                "role": "USER",
                "birthDate": "1990-01-01T00:00:00.000Z", # Ensure date format is correct
                "isPrivate": false,
                "profile": "https://example.com/profile.jpg"
              }'
        ```
    *   **Response Body (200 OK):**
        ```json
        {
          "message": "join successfully"
        }
        ```

#### 2. User ID Duplicate Check

*   **3.3.2.1. 요청 (Request)**
    *   **HTTP Method:** `POST`
    *   **Endpoint Path:** `/api/users/public/check/userId/IsDuplicate` (Path from `UsersController.java`, `API_Documentation.md` is similar)
    *   **Full URL 예시:** `https://<your-domain>/api/users/public/check/userId/IsDuplicate`
    *   **기능 설명:** Checks if a given User ID (login ID) is already in use.
    *   **Path Parameters:** None.
    *   **Query Parameters:** None.
    *   **Header Parameters:** None.
    *   **Request Body:**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `UserNameCheckRequestDto.java` (`com.authentication.auth.dto.users.UserNameCheckRequestDto`) - Note: DTO name is `UserNameCheckRequestDto`, field is `userName`.
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명    | 데이터 타입 | 제약조건 | 설명             | 예시           | 필수 |
            |-----------|-------------|----------|------------------|----------------|------|
            | `userName`| String      | Required | 확인할 사용자 ID | "testuser123"  | Yes  |

*   **3.3.2.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Check performed successfully.
    *   **Response Headers:** None specific.
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json` (Controller returns `ResponseEntity<Boolean>`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            Boolean value: `true` if duplicate, `false` if available.
            ```json
            true
            ```
            or
            ```json
            false
            ```

*   **3.3.2.3. 인증 및 인가:**
    *   인증 필요 여부: No.
    *   필요한 권한: None.

*   **3.3.2.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `UsersController.java` (`com.authentication.auth.controller.UsersController.checkUserIdIsDuplicate()`)
    *   **Service:** `UserService.java` (`com.authentication.auth.service.users.UserService.checkUserNameIsDuplicate()`)
    *   **Domain/Entity:** `User.java`
    *   **DTO:** `UserNameCheckRequestDto.java`
    *   **Repository:** `UserRepository.java`

*   **3.3.2.5. 예외 처리 (Error Handling):**
    *   Generally returns boolean, specific error responses might not be typical for this check unless server error.

*   **3.3.2.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X POST \
          https://<your-domain>/api/users/public/check/userId/IsDuplicate \
          -H 'Content-Type: application/json' \
          -d '{
                "userName": "existinguser"
              }'
        ```
    *   **Response Body (200 OK):**
        ```json
        true
        ```

---
### 3.3.C. Email API

(Details for Email APIs like Send Code, Verify Code would follow a similar structure, using `EmailRequest.java`, `EmailCheckDto.java`, etc. as DTOs. Referencing `EmailController.java` and `EmailService.java`)

---
### 3.3.D. Diary API (New and Existing)

#### 1. Create Diary Entry

*   **3.3.D.1.1. 요청 (Request)**
    *   **HTTP Method:** `POST`
    *   **Endpoint Path:** `/api/diaries`
    *   **Full URL 예시:** `https://<your-domain>/api/v1/diaries` (assuming v1)
    *   **기능 설명:** Creates a new diary entry for the authenticated user.
    *   **Path Parameters:** None.
    *   **Query Parameters:** None.
    *   **Header Parameters:**
        *   `Authorization`: `Bearer {access_token}` (Mandatory)
    *   **Request Body:**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `DiaryCreateRequest.java` (`com.authentication.auth.dto.diary.DiaryCreateRequest`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명    | 데이터 타입 | 제약조건                      | 설명             | 예시                     | 필수 |
            |-----------|-------------|-------------------------------|------------------|--------------------------|------|
            | `title`   | String      | Max 255자                   | 일기 제목 (선택) | "오늘의 일기"            | No   |
            | `content` | String      | NotBlank, Max 5000자        | 일기 내용        | "오늘은 정말 즐거운 하루였다." | Yes  |

*   **3.3.D.1.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `201 Created`: Diary entry created successfully.
        *   `400 Bad Request`: Invalid request data (e.g., missing content).
        *   `401 Unauthorized`: User not authenticated.
        *   `500 Internal Server Error`: Server-side error.
    *   **Response Headers:**
        *   `Location`: URL of the newly created diary entry (e.g., `/api/v1/diaries/{diaryId}`).
    *   **Response Body (201 Created):**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `DiaryResponse.java` (`com.authentication.auth.dto.diary.DiaryResponse`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명               | 데이터 타입     | 설명                               | 예시                               |
            |----------------------|-----------------|------------------------------------|------------------------------------|
            | `id`                 | Long            | 생성된 일기 ID                     | 123                                |
            | `userId`             | Long            | 작성자 User ID                     | 1                                  |
            | `title`              | String          | 일기 제목                          | "오늘의 일기"                      |
            | `content`            | String          | 일기 내용                          | "오늘은 정말 즐거운 하루였다."       |
            | `alternativeThought` | String          | AI 추천 대체 생각 (초기엔 null)    | null                               |
            | `isNegative`         | Boolean         | 부정 감정 여부 (초기엔 false/null) | false                              |
            | `createdAt`          | LocalDateTime   | 생성 시간 (ISO 8601)               | "2023-05-01T12:00:00Z"             |
            | `updatedAt`          | LocalDateTime   | 수정 시간 (ISO 8601)               | "2023-05-01T12:00:00Z"             |

*   **3.3.D.1.3. 인증 및 인가:**
    *   인증 필요 여부: Yes, Bearer Token.
    *   필요한 권한: Standard user role (e.g., "USER").

*   **3.3.D.1.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `DiaryController.java` (Not found in codebase, assumed based on API docs)
    *   **Service:** `DiaryService.java` (Not found in codebase, assumed) - Would handle saving and potentially sending to Kafka for analysis.
    *   **Domain/Entity:** `Diary.java` (`com.authentication.auth.domain.Diary`), `User.java`
    *   **DTO:** `DiaryCreateRequest.java`, `DiaryResponse.java`
    *   **Repository:** `DiaryRepository.java` (Assumed), `UserRepository.java`

*   **3.3.D.1.5. 예외 처리 (Error Handling):**
    *   `400 Bad Request`: Missing `content`.
    *   `401 Unauthorized`: Invalid or missing token.

*   **3.3.D.1.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X POST \
          https://<your-domain>/api/v1/diaries \
          -H 'Authorization: Bearer your_access_token' \
          -H 'Content-Type: application/json' \
          -d '{
                "title": "A wonderful day",
                "content": "Spent the day coding and learned a lot!"
              }'
        ```
    *   **Response Body (201 Created):**
        ```json
        {
          "id": 123,
          "userId": 1,
          "title": "A wonderful day",
          "content": "Spent the day coding and learned a lot!",
          "alternativeThought": null,
          "isNegative": false,
          "createdAt": "2023-10-27T10:00:00Z",
          "updatedAt": "2023-10-27T10:00:00Z"
        }
        ```

#### 2. List User's Diary Entries

*   **3.3.D.2.1. 요청 (Request)**
    *   **HTTP Method:** `GET`
    *   **Endpoint Path:** `/api/diaries`
    *   **Full URL 예시:** `https://<your-domain>/api/v1/diaries?page=0&size=10&sort=createdAt,desc`
    *   **기능 설명:** Retrieves a paginated list of diary entries for the authenticated user.
    *   **Path Parameters:** None.
    *   **Query Parameters:**
        | 필드명 | 데이터 타입 | 제약조건 | 설명                                   | 예시             | 필수 |
        |--------|-------------|----------|----------------------------------------|------------------|------|
        | `page` | Integer     |          | 페이지 번호 (0-indexed, default: 0)    | 0                | No   |
        | `size` | Integer     |          | 페이지 당 항목 수 (default: 10)        | 10               | No   |
        | `sort` | String      |          | 정렬 기준 (property,direction)         | "createdAt,desc" | No   |
    *   **Header Parameters:**
        *   `Authorization`: `Bearer {access_token}` (Mandatory)
    *   **Request Body:** None.

*   **3.3.D.2.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Successfully retrieved list of diaries.
        *   `401 Unauthorized`: User not authenticated.
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** Paginated response containing `DiaryListItem.java` (`com.authentication.auth.dto.diary.DiaryListItem`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            ```json
            {
              "diaries": [ // List of DiaryListItem
                {
                  "id": 123,
                  "title": "A wonderful day",
                  "createdAt": "2023-10-27T10:00:00Z",
                  "emotionStatus": "POSITIVE" // or "NEGATIVE", "NEUTRAL"
                }
                // ... more items
              ],
              "pageInfo": {
                "currentPage": 0,
                "totalPages": 5,
                "totalElements": 48,
                "size": 10
              }
            }
            ```

*   **3.3.D.2.3. 인증 및 인가:**
    *   인증 필요 여부: Yes, Bearer Token.
    *   필요한 권한: Standard user role.

*   **3.3.D.2.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `DiaryController.java` (Not found)
    *   **Service:** `DiaryService.java` (Not found)
    *   **Domain/Entity:** `Diary.java`
    *   **DTO:** `DiaryListItem.java`
    *   **Repository:** `DiaryRepository.java` (Assumed)

*   **3.3.D.2.5. 예외 처리 (Error Handling):**
    *   `401 Unauthorized`: Invalid or missing token.

*   **3.3.D.2.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X GET \
          'https://<your-domain>/api/v1/diaries?page=0&size=2' \
          -H 'Authorization: Bearer your_access_token'
        ```
    *   **Response Body (200 OK):**
        ```json
        {
          "diaries": [
            {
              "id": 124,
              "title": "Feeling thoughtful",
              "createdAt": "2023-10-28T15:30:00Z",
              "emotionStatus": "NEUTRAL"
            },
            {
              "id": 123,
              "title": "A wonderful day",
              "createdAt": "2023-10-27T10:00:00Z",
              "emotionStatus": "POSITIVE"
            }
          ],
          "pageInfo": {
            "currentPage": 0,
            "totalPages": 10,
            "totalElements": 20,
            "size": 2
          }
        }
        ```

#### 3. Get Single Diary Entry

*   **3.3.D.3.1. 요청 (Request)**
    *   **HTTP Method:** `GET`
    *   **Endpoint Path:** `/api/diaries/{diaryId}`
    *   **Full URL 예시:** `https://<your-domain>/api/v1/diaries/123`
    *   **기능 설명:** Retrieves a single diary entry by its ID, including AI analysis results.
    *   **Path Parameters:**
        | 필드명    | 데이터 타입 | 필수 여부 | 설명          | 예시 |
        |-----------|-------------|-----------|---------------|------|
        | `diaryId` | Long        | Yes       | 조회할 일기 ID | 123  |
    *   **Query Parameters:** None.
    *   **Header Parameters:**
        *   `Authorization`: `Bearer {access_token}` (Mandatory)
    *   **Request Body:** None.

*   **3.3.D.3.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Successfully retrieved the diary entry.
        *   `401 Unauthorized`: User not authenticated.
        *   `403 Forbidden`: User not authorized to access this diary.
        *   `404 Not Found`: Diary entry with the given ID not found.
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `DiaryDetailResponse.java` (`com.authentication.auth.dto.diary.DiaryDetailResponse`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명                    | 데이터 타입              | 설명                                  | 예시                                      |
            |---------------------------|--------------------------|---------------------------------------|-------------------------------------------|
            | `id`                      | Long                     | 일기 ID                               | 123                                       |
            | `userId`                  | Long                     | 작성자 User ID                        | 1                                         |
            | `title`                   | String                   | 일기 제목                             | "오늘의 일기"                             |
            | `content`                 | String                   | 일기 내용                             | "오늘은 정말 즐거운 하루였다."            |
            | `alternativeThoughtByAI`  | String                   | AI 추천 대체 생각 (from Diary entity) | "다른 관점에서 보면..."                   |
            | `isNegative`              | Boolean                  | 부정 감정 여부 (from Diary entity)    | false                                     |
            | `createdAt`               | LocalDateTime            | 생성 시간 (ISO 8601)                  | "2023-05-01T12:00:00Z"                    |
            | `updatedAt`               | LocalDateTime            | 수정 시간 (ISO 8601)                  | "2023-05-01T12:05:00Z"                    |
            | `analysis`                | `DiaryAnalysisResultDto` | AI 분석 결과                          | (See `DiaryAnalysisResultDto` structure)  |
            *   `DiaryAnalysisResultDto` structure (based on API Docs & `Data_Models_Entities.md` for `DiaryAnswer`):
                | 필드명                | 데이터 타입 | 설명                               | 예시                                                        |
                |-----------------------|-------------|------------------------------------|-------------------------------------------------------------|
                | `id`                  | Long        | 분석 결과 ID                       | 42                                                          |
                | `emotionDetection`    | String      | 감지된 감정 (e.g., text summary)   | "기쁨 80%, 슬픔 10%"                                         |
                | `automaticThought`    | String      | 감지된 자동적 사고                 | "나는 항상 실패한다."                                       |
                | `promptForChange`     | String      | 변화를 위한 프롬프트               | "정말 항상 실패했나요?"                                     |
                | `alternativeThought`  | String      | 제안된 대체 생각 (from analysis)   | "과거에 몇 번 실패했지만, 그것이 항상 실패한다는 의미는 아니다." |
                | `status`              | String      | 감정 상태 (POSITIVE/NEGATIVE/NEUTRAL)| "POSITIVE"                                                  |
                | `analyzedAt`          | LocalDateTime | 분석 완료 시간 (ISO 8601)          | "2023-05-01T12:05:00Z"                                      |

*   **3.3.D.3.3. 인증 및 인가:**
    *   인증 필요 여부: Yes, Bearer Token.
    *   필요한 권한: User must be the owner of the diary entry.

*   **3.3.D.3.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `DiaryController.java` (Not found)
    *   **Service:** `DiaryService.java` (Not found) - Would fetch Diary and its related DiaryAnswer/Analysis.
    *   **Domain/Entity:** `Diary.java`, `DiaryAnswer.java` (or equivalent analysis entity)
    *   **DTO:** `DiaryDetailResponse.java`, `DiaryAnalysisResultDto.java`
    *   **Repository:** `DiaryRepository.java` (Assumed), `DiaryAnswerRepository.java` (Assumed)

*   **3.3.D.3.5. 예외 처리 (Error Handling):**
    *   `401 Unauthorized`, `403 Forbidden`, `404 Not Found`.

*   **3.3.D.3.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X GET \
          https://<your-domain>/api/v1/diaries/123 \
          -H 'Authorization: Bearer your_access_token'
        ```
    *   **Response Body (200 OK):**
        ```json
        {
          "id": 123,
          "userId": 1,
          "title": "Feeling Conflicted",
          "content": "Not sure about the project direction.",
          "alternativeThoughtByAI": "It's okay to feel unsure, maybe discussing with the team will help clarify.",
          "isNegative": true,
          "createdAt": "2023-10-27T14:00:00Z",
          "updatedAt": "2023-10-27T14:00:00Z",
          "analysis": {
            "id": 45,
            "emotionDetection": "불안 60%, 혼란 30%",
            "automaticThought": "이 프로젝트는 실패할 거야.",
            "promptForChange": "실패라고 단정하기 전에 어떤 점이 우려되는지 구체적으로 생각해볼까요?",
            "alternativeThought": "우려되는 점들이 있지만, 팀과 논의하여 해결책을 찾을 수 있다.",
            "status": "NEGATIVE",
            "analyzedAt": "2023-10-27T14:05:00Z"
          }
        }
        ```

#### 4. Update Diary Entry

*   **3.3.D.4.1. 요청 (Request)**
    *   **HTTP Method:** `PUT`
    *   **Endpoint Path:** `/api/diaries/{diaryId}`
    *   **Full URL 예시:** `https://<your-domain>/api/v1/diaries/123`
    *   **기능 설명:** Updates an existing diary entry.
    *   **Path Parameters:**
        | 필드명    | 데이터 타입 | 필수 여부 | 설명          | 예시 |
        |-----------|-------------|-----------|---------------|------|
        | `diaryId` | Long        | Yes       | 수정할 일기 ID | 123  |
    *   **Query Parameters:** None.
    *   **Header Parameters:**
        *   `Authorization`: `Bearer {access_token}` (Mandatory)
    *   **Request Body:**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `DiaryCreateRequest.java` (as `DiaryUpdateRequest.java` was not found, and API docs suggest same fields: title, content).
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명    | 데이터 타입 | 제약조건                      | 설명             | 예시                     | 필수 |
            |-----------|-------------|-------------------------------|------------------|--------------------------|------|
            | `title`   | String      | Max 255자                   | 일기 제목 (선택) | "수정된 오늘의 일기"       | No   |
            | `content` | String      | NotBlank, Max 5000자        | 일기 내용        | "내용이 일부 수정되었습니다." | Yes  |

*   **3.3.D.4.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Diary entry updated successfully.
        *   `400 Bad Request`: Invalid request data.
        *   `401 Unauthorized`: User not authenticated.
        *   `403 Forbidden`: User not authorized to update this diary.
        *   `404 Not Found`: Diary entry not found.
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `DiaryResponse.java`
        *   **JSON 스키마 (Same as Create Diary Response):** (id, userId, title, content, alternativeThought, isNegative, createdAt, updatedAt)

*   **3.3.D.4.3. 인증 및 인가:**
    *   인증 필요 여부: Yes, Bearer Token.
    *   필요한 권한: User must be the owner of the diary entry.

*   **3.3.D.4.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `DiaryController.java` (Not found)
    *   **Service:** `DiaryService.java` (Not found) - Would handle updating and potentially re-triggering analysis if content changed significantly.
    *   **Domain/Entity:** `Diary.java`
    *   **DTO:** `DiaryCreateRequest.java` (acting as update DTO), `DiaryResponse.java`
    *   **Repository:** `DiaryRepository.java` (Assumed)

*   **3.3.D.4.5. 예외 처리 (Error Handling):**
    *   `400`, `401`, `403`, `404`.

*   **3.3.D.4.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X PUT \
          https://<your-domain>/api/v1/diaries/123 \
          -H 'Authorization: Bearer your_access_token' \
          -H 'Content-Type: application/json' \
          -d '{
                "title": "Updated title",
                "content": "Updated content for the diary."
              }'
        ```
    *   **Response Body (200 OK):**
        ```json
        {
          "id": 123,
          "userId": 1,
          "title": "Updated title",
          "content": "Updated content for the diary.",
          "alternativeThought": "It's okay to feel unsure, maybe discussing with the team will help clarify.", // Assuming analysis is not re-run on simple text update by default
          "isNegative": true,
          "createdAt": "2023-10-27T14:00:00Z",
          "updatedAt": "2023-10-27T18:30:00Z" // Note updated time
        }
        ```

#### 5. Delete Diary Entry

*   **3.3.D.5.1. 요청 (Request)**
    *   **HTTP Method:** `DELETE`
    *   **Endpoint Path:** `/api/diaries/{diaryId}`
    *   **Full URL 예시:** `https://<your-domain>/api/v1/diaries/123`
    *   **기능 설명:** Deletes a specific diary entry.
    *   **Path Parameters:**
        | 필드명    | 데이터 타입 | 필수 여부 | 설명          | 예시 |
        |-----------|-------------|-----------|---------------|------|
        | `diaryId` | Long        | Yes       | 삭제할 일기 ID | 123  |
    *   **Query Parameters:** None.
    *   **Header Parameters:**
        *   `Authorization`: `Bearer {access_token}` (Mandatory)
    *   **Request Body:** None.

*   **3.3.D.5.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `204 No Content`: Diary entry deleted successfully.
        *   `401 Unauthorized`: User not authenticated.
        *   `403 Forbidden`: User not authorized to delete this diary.
        *   `404 Not Found`: Diary entry not found.
    *   **Response Body:** None for `204 No Content`.

*   **3.3.D.5.3. 인증 및 인가:**
    *   인증 필요 여부: Yes, Bearer Token.
    *   필요한 권한: User must be the owner of the diary entry.

*   **3.3.D.5.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `DiaryController.java` (Not found)
    *   **Service:** `DiaryService.java` (Not found)
    *   **Domain/Entity:** `Diary.java`
    *   **DTO:** None
    *   **Repository:** `DiaryRepository.java` (Assumed)

*   **3.3.D.5.5. 예외 처리 (Error Handling):**
    *   `401`, `403`, `404`.

*   **3.3.D.5.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X DELETE \
          https://<your-domain>/api/v1/diaries/123 \
          -H 'Authorization: Bearer your_access_token'
        ```
    *   **Response:** `HTTP/1.1 204 No Content`

---
### 3.3.E. Settings API (New and Existing)

#### 1. Get User Settings

*   **3.3.E.1.1. 요청 (Request)**
    *   **HTTP Method:** `GET`
    *   **Endpoint Path:** `/api/settings`
    *   **Full URL 예시:** `https://<your-domain>/api/v1/settings`
    *   **기능 설명:** Retrieves all application settings, including current values for the authenticated user.
    *   **Path Parameters:** None.
    *   **Query Parameters:** None.
    *   **Header Parameters:**
        *   `Authorization`: `Bearer {access_token}` (Mandatory)
    *   **Request Body:** None.

*   **3.3.E.1.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Settings retrieved successfully.
        *   `401 Unauthorized`: User not authenticated.
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `SettingsListResponse.java` (`com.authentication.auth.dto.settings.SettingsListResponse`) containing a list of `SettingItem.java`.
        *   **JSON 스키마 또는 필드별 상세 설명 (`SettingItem`):**
            | 필드명           | 데이터 타입 | 설명                                     | 예시                       |
            |------------------|-------------|------------------------------------------|----------------------------|
            | `settingKey`     | String      | 고유 설정 키                             | "notification.enabled"     |
            | `value`          | Any         | 현재 설정 값 (사용자 설정 또는 기본값)   | true                       |
            | `dataType`       | String      | 값의 데이터 타입 (STRING, BOOLEAN, NUMBER) | "BOOLEAN"                  |
            | `description`    | String      | 설정 설명                                | "알림 활성화 여부"         |
            | `isUserEditable` | boolean     | 사용자가 수정 가능한지 여부              | true                       |
            **`SettingsListResponse` Example:**
            ```json
            {
              "settings": [
                {
                  "settingKey": "notification.enabled",
                  "value": true,
                  "dataType": "BOOLEAN",
                  "description": "Enable or disable all notifications.",
                  "isUserEditable": true
                },
                {
                  "settingKey": "theme.color",
                  "value": "dark",
                  "dataType": "STRING",
                  "description": "Application theme color (dark/light).",
                  "isUserEditable": true
                }
              ]
            }
            ```

*   **3.3.E.1.3. 인증 및 인가:**
    *   인증 필요 여부: Yes, Bearer Token.
    *   필요한 권한: Standard user role.

*   **3.3.E.1.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `SettingsController.java` (Not found in codebase, assumed based on API docs)
    *   **Service:** `SettingsService.java` (Not found in codebase, assumed) - Would fetch all `SettingsOption` and override with `UserCustomSetting` for the current user.
    *   **Domain/Entity:** `SettingsOption.java`, `UserCustomSetting.java`, `User.java`
    *   **DTO:** `SettingsListResponse.java`, `SettingItem.java`
    *   **Repository:** `SettingsOptionRepository.java` (Assumed), `UserCustomSettingRepository.java` (Assumed)

*   **3.3.E.1.5. 예외 처리 (Error Handling):**
    *   `401 Unauthorized`.

*   **3.3.E.1.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X GET \
          https://<your-domain>/api/v1/settings \
          -H 'Authorization: Bearer your_access_token'
        ```
    *   **Response Body (200 OK):** (As shown in schema example above)

#### 2. Update User Settings

*   **3.3.E.2.1. 요청 (Request)**
    *   **HTTP Method:** `PUT`
    *   **Endpoint Path:** `/api/settings`
    *   **Full URL 예시:** `https://<your-domain>/api/v1/settings`
    *   **기능 설명:** Updates one or more settings for the authenticated user.
    *   **Path Parameters:** None.
    *   **Query Parameters:** None.
    *   **Header Parameters:**
        *   `Authorization`: `Bearer {access_token}` (Mandatory)
    *   **Request Body:**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `SettingsUpdateRequest.java` (`com.authentication.auth.dto.settings.SettingsUpdateRequest`)
        *   **JSON 스키마 (`SettingsUpdateRequestItem` within the list):**
            | 필드명       | 데이터 타입 | 제약조건 | 설명         | 예시                       | 필수 |
            |--------------|-------------|----------|--------------|----------------------------|------|
            | `settingKey` | String      | NotBlank | 수정할 설정 키 | "notification.enabled"     | Yes  |
            | `newValue`   | Any         | NotNull  | 새로운 설정 값 | false                      | Yes  |
            **`SettingsUpdateRequest` Example:**
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
              ]
            }
            ```

*   **3.3.E.2.2. 응답 (Response)**
    *   **HTTP Status Code 별 응답 정의:**
        *   `200 OK`: Settings updated successfully.
        *   `400 Bad Request`: Invalid request data (e.g., unknown setting key, value type mismatch).
        *   `401 Unauthorized`: User not authenticated.
    *   **Response Body (200 OK):**
        *   **Content-Type:** `application/json`
        *   **연결된 DTO 클래스명:** `SettingsUpdateResponse.java` (`com.authentication.auth.dto.settings.SettingsUpdateResponse`)
        *   **JSON 스키마 또는 필드별 상세 설명:**
            | 필드명           | 데이터 타입          | 설명                         | 예시                                       |
            |------------------|----------------------|------------------------------|--------------------------------------------|
            | `message`        | String               | 성공 메시지                  | "Settings updated successfully"            |
            | `updatedSettings`| List of `SettingItem`| 업데이트된 설정 항목들의 목록 | (List of `SettingItem` like in GET response) |

*   **3.3.E.2.3. 인증 및 인가:**
    *   인증 필요 여부: Yes, Bearer Token.
    *   필요한 권한: Standard user role. Only `isUserEditable=true` settings can be changed.

*   **3.3.E.2.4. 연관 컴포넌트 추적 (Traceability):**
    *   **Controller:** `SettingsController.java` (Not found)
    *   **Service:** `SettingsService.java` (Not found) - Would validate and save `UserCustomSetting`.
    *   **Domain/Entity:** `SettingsOption.java`, `UserCustomSetting.java`
    *   **DTO:** `SettingsUpdateRequest.java`, `SettingsUpdateRequestItem.java`, `SettingsUpdateResponse.java`, `SettingItem.java`
    *   **Repository:** `SettingsOptionRepository.java` (Assumed), `UserCustomSettingRepository.java` (Assumed)

*   **3.3.E.2.5. 예외 처리 (Error Handling):**
    *   `400 Bad Request`: Invalid `settingKey`, or `newValue` doesn't match `dataType`, or setting is not user-editable.
    *   `401 Unauthorized`.

*   **3.3.E.2.6. 호출 예시 (Sample Request/Response):**
    *   **Request (cURL):**
        ```bash
        curl -X PUT \
          https://<your-domain>/api/v1/settings \
          -H 'Authorization: Bearer your_access_token' \
          -H 'Content-Type: application/json' \
          -d '{
                "settingsToUpdate": [
                  {
                    "settingKey": "theme.color",
                    "newValue": "light"
                  }
                ]
              }'
        ```
    *   **Response Body (200 OK):**
        ```json
        {
          "message": "Settings updated successfully",
          "updatedSettings": [
            {
              "settingKey": "theme.color",
              "value": "light",
              "dataType": "STRING",
              "description": "Application theme color (dark/light).",
              "isUserEditable": true
            }
          ]
        }
        ```

---
(Other APIs like OAuth2, SSE would be detailed similarly, using their respective DTOs and controller information from previous steps and `API_Documentation.md`.)
I will acknowledge that DTOs for Diary and Settings were found and used, which is better than inferring. The `DiaryUpdateRequest.java` was not found, so `DiaryCreateRequest.java` was used for the update operation as its structure matched the documented fields for update.
Controller and Service classes for Diary and Settings APIs were noted as not found in the current codebase.
