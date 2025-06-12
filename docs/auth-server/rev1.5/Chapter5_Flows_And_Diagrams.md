# Chapter 5: Main Feature Flowcharts and Sequence Diagrams

_Note: This chapter provides textual descriptions and step-by-step outlines for flowcharts and sequence diagrams, as visual diagram generation is not possible and image files (`abstract-flow.png`, `abstract-flow-alter.jpg`) were not found in the repository._

## 5.1. User Scenario based Flowchart Descriptions

These descriptions outline the sequence of events, component interactions, API calls, and data read/write (R/W) operations for key user scenarios.

### 5.1.1. 회원가입 흐름 (Email Verification Included) - User Registration Flow

1.  **User Input (Client):**
    *   User accesses the registration page/form on the Client (Web/Mobile App).
    *   User enters registration details: User ID (login ID), password, name/nickname, email address, phone number, gender, etc.
2.  **Request Email Verification Code (Client -> Backend):**
    *   Client sends an API request to the Backend to initiate email verification.
    *   **API Call:** `POST /api/public/emailSend` (or similar, e.g., `/api/users/public/email/send-verification`)
    *   Request Body: `{ "email": "user@example.com" }`
3.  **Process Email Verification (Backend - Auth Service / Email Service):**
    *   Auth Service receives the request.
    *   Generates a unique verification code.
    *   **Data Write (Redis):** Stores the verification code in Redis with the user's email as the key and a Time-To-Live (TTL) (e.g., 30 minutes). `RedisService.saveEmailCode()`.
    *   **Async Email Sending:** Calls an Email Service (e.g., `EmailService.sendVerificationEmail()`) to send an email containing the verification code to the user's provided email address.
    *   Backend responds to Client (e.g., `200 OK {"message": "Verification code sent"}`).
4.  **User Enters Code (Client):**
    *   User receives the verification email.
    *   User copies the code and enters it into the verification field on the Client application.
5.  **Submit Verification Code (Client -> Backend):**
    *   Client sends the entered verification code and email to the Backend for validation.
    *   **API Call:** `POST /api/public/emailCheck` (or similar, e.g., `/api/users/public/email/verify-code`)
    *   Request Body: `{ "email": "user@example.com", "code": "A1B2C3D4" }`
6.  **Validate Code (Backend - Auth Service):**
    *   Auth Service receives the request.
    *   **Data Read (Redis):** Retrieves the stored verification code from Redis using the email as the key. `RedisService.checkEmailCode()`.
    *   Compares the submitted code with the stored code.
    *   If valid and not expired, responds with success (e.g., `202 Accepted {"message": "Email verified successfully"}`).
    *   If invalid or expired, responds with an error (e.g., `400 Bad Request {"message": "Invalid or expired code"}`).
7.  **Submit Full Registration (Client -> Backend):**
    *   Assuming email verification was successful, the Client now submits the full registration form data.
    *   **API Call:** `POST /api/users/public/join` (as per `UsersController.java` and API docs).
    *   Request Body: Contains all user details (userId, userPw, email, phone, gender, etc.) including the verified email and the verification code itself (as per `JoinRequest.java` DTO).
8.  **Process Registration (Backend - User Service):**
    *   `UsersController` receives the request.
    *   `UserService.join()` is called:
        *   Validates input data (e.g., duplicate userId/email check - `UserRepository.existsByUserName()`).
        *   Hashes the password using `BCryptPasswordEncoder`.
        *   **Data Write (MariaDB):** Creates a new `User` entity and saves it to the `Users` table in MariaDB. The user's `isActive` status might be set to 'WAITING' or 'ACTIVE' depending on system policy post-email verification (entity default is 'WAITING', `JoinRequest` sets to 'WAITING').
        *   **Async Event (Kafka):** Publishes a `UserCreatedEvent` to a Kafka topic (e.g., "user-created-events") via `UserEventProducer`. This can be used for downstream processes like setting up user profiles in other services or analytics.
9.  **Registration Response (Backend -> Client):**
    *   Backend responds to Client with success (e.g., `200 OK {"message": "join successfully"}`) or failure (e.g., `409 Conflict {"message": "already exist userId or nickname"}`).

### 5.1.2. 로컬 로그인 및 JWT 발급 흐름 - Local Login and JWT Issuance Flow

1.  **User Input (Client):**
    *   User accesses the login page/form on the Client.
    *   User enters their credentials: User ID (login ID) and password.
2.  **Login Request (Client -> Backend):**
    *   Client sends an API request to the Backend to authenticate the user.
    *   **API Call:** `POST /api/auth/login`
    *   Request Body: `{ "userId": "testuser123", "password": "P@sswOrd!" }` (DTO: `LoginRequest.java`)
3.  **Authentication Processing (Backend - Spring Security):**
    *   The request is intercepted by `AuthenticationFilter` (configured for `/api/auth/login`).
    *   `AuthenticationFilter.attemptAuthentication()`:
        *   Extracts `userId` and `password` from the request.
        *   Creates a `UsernamePasswordAuthenticationToken`.
        *   Calls `AuthenticationManager.authenticate()`.
    *   `ProviderManager` (default `AuthenticationManager`) uses `DaoAuthenticationProvider`:
        *   Calls `UserDetailsService.loadUserByUsername()` (implemented by `PrincipalDetailService`).
        *   **Data Read (MariaDB):** `PrincipalDetailService` fetches the `User` entity from the `Users` table in MariaDB via `UserRepository.findByUserName()`.
        *   `DaoAuthenticationProvider` compares the provided password (hashed) with the stored hashed password using `BCryptPasswordEncoder.matches()`.
4.  **JWT Generation & Storage (Backend - Auth Service / Token Provider):**
    *   If authentication is successful, `AuthenticationFilter.successfulAuthentication()` is called:
        *   Retrieves `PrincipalDetails` (containing User information) from the `Authentication` object.
        *   Calls `TokenProvider.buildToken()` (which uses `JwtUtility`) to generate:
            *   **Access Token:** Short-lived JWT containing user ID, roles, expiration.
            *   **Refresh Token:** Longer-lived JWT.
        *   **Data Write (Redis):** The generated Refresh Token is stored in Redis via `RedisService.saveRToken()`, keyed by `userId` and `provider` ("server"), with a configured TTL.
5.  **Login Response (Backend -> Client):**
    *   **Response Headers:**
        *   `Set-Cookie`: `refreshToken={refresh_token_value}; Path=/; HttpOnly; Secure; Max-Age={expiry}` - The Refresh Token is set as an HttpOnly cookie.
        *   `Set-Cookie`: `accessToken={access_token_value}; Path=/; HttpOnly; Secure; Max-Age={expiry}` - The Access Token is also set as an HttpOnly cookie by `AuthenticationFilter`.
    *   **Response Body:** `200 OK`
        *   Contains the Access Token and Refresh Token (DTO: `TokenDto.java`).
        ```json
        {
          "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
          "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
        }
        ```
6.  **Client Stores Tokens:**
    *   Client receives the tokens. The Refresh Token is stored securely by the browser (HttpOnly cookie). The Access Token from the body is typically stored in memory (e.g., JavaScript variable) or secure local storage by the client application for use in subsequent API calls.

### 5.1.3. 소셜 로그인 (OAuth2) 흐름 (Provider별 - Kakao, Naver, Google) - Social Login Flow

1.  **User Initiates Social Login (Client):**
    *   User clicks a "Login with Google/Kakao/Naver" button on the Client.
2.  **Request OAuth2 Login URL (Client -> Backend):**
    *   Client makes an API call to the Backend to get the OAuth2 provider's authorization URL.
    *   **API Call:** `GET /api/public/oauth2/login_url/{provider}` (e.g., `{provider}` is "google"). (Handled by `AuthController.java` as per API docs).
3.  **Return Redirect URL (Backend -> Client):**
    *   Backend constructs the appropriate authorization URL for the specified provider (including client ID, redirect URI, scopes, etc.).
    *   Responds with the URL: `200 OK {"login_url": "https://accounts.google.com/o/oauth2/v2/auth?..."}`.
4.  **Redirect to Provider (Client -> OAuth2 Provider):**
    *   Client redirects the user's browser to the received `login_url`.
5.  **User Authentication at Provider (User <-> OAuth2 Provider):**
    *   User authenticates on the OAuth2 provider's site (e.g., enters Google credentials).
    *   User grants permission for the application to access requested profile information.
6.  **Provider Redirects to Callback (OAuth2 Provider -> Client -> Backend):**
    *   OAuth2 provider redirects the user's browser back to the application's registered callback URL, including an authorization `code` and `state` (if used).
    *   **Callback URL:** e.g., `https://<your-domain>/api/public/oauth2/callback/{provider}?code=AUTHORIZATION_CODE&state=STATE_STRING` (GET request, as per API docs for generic callback).
    *   The client might intercept this or it might directly hit the backend. The `Oauth2Controller` has POST endpoints like `/oauth2/callback/google` which expect `tempCode` in the body, suggesting the client might forward this code. Assuming the client forwards the code to the backend POST endpoint.
7.  **Handle OAuth2 Callback (Client -> Backend - Oauth2Controller):**
    *   Client sends the received `code` (as `tempCode`) to the backend.
    *   **API Call:** `POST /oauth2/callback/{provider}` (e.g., `/oauth2/callback/google`)
    *   Request Body: `{ "tempCode": "AUTHORIZATION_CODE_FROM_PROVIDER" }`
8.  **Process Callback (Backend - Oauth2Service):**
    *   `Oauth2Controller` calls `Oauth2Service.handleOauth2Login()`.
    *   `Oauth2Service`:
        *   **Token Exchange:** Exchanges the `tempCode` (authorization code) with the OAuth2 provider for an
            OAuth Access Token and potentially an OAuth Refresh Token. This involves a direct server-to-server call to the provider's token endpoint.
        *   **Fetch User Profile:** Uses the obtained OAuth Access Token to request user profile data from the provider's user info endpoint.
        *   **User Lookup/Registration (Data R/W - MariaDB):**
            *   Calls `saveOrUpdateOauth2User()`:
                *   Searches for an existing `UserAuthentication` record by provider and social ID.
                *   If found, retrieves the associated `User`. Updates user's email or other details if changed.
                *   If not found, creates a new `User` entity (populating email, derived username, default role "USER", status "ACTIVE") and a new `UserAuthentication` entity linking the user to the provider and social ID. Saves both to MariaDB (`UserRepository`, `UserAuthenticationRepository`).
        *   **Store Provider's Refresh Token (Data Write - Redis):** If the provider issued a refresh token, `Oauth2Service` stores it in Redis, associated with the user's social ID and provider (`RedisService.saveRToken()`).
        *   **Generate Application JWTs:** Calls `JwtUtility.buildToken()` to create the application's own Access Token and Refresh Token for the authenticated/registered user.
        *   **Store Application Refresh Token (Data Write - Redis & Cookie):** The application's Refresh Token is stored in Redis (`RedisService.saveRToken()` with provider "server" or the social provider name) and set as an HttpOnly cookie in the HTTP response.
9.  **Return Application Tokens (Backend -> Client):**
    *   Responds with `200 OK`.
    *   Response Body: Contains the application's Access Token and user profile information (DTO: `OAuth2LoginResponse.java`).
    ```json
    {
      "access_token": "APPLICATION_ACCESS_TOKEN",
      "userProfile": { /* user profile details */ }
    }
    ```
10. **Client Stores Tokens:** Similar to local login, client stores the application's Access Token for API calls.

### 5.1.4. JWT 토큰 재발급 흐름 - JWT Refresh Token Flow

1.  **Access Token Expiration (Client & API):**
    *   Client makes an API request to a protected endpoint using an expired Access Token in the `Authorization` header.
    *   Backend API (guarded by `JwtVerificationFilter` or similar) detects the token is expired and returns a `401 Unauthorized` error.
2.  **Request Token Refresh (Client -> Backend):**
    *   Client, upon receiving `401`, uses its stored (expired) Access Token and the Refresh Token (obtained from the HttpOnly cookie, if accessible by client-side JS, or client relies on server to use cookie) to request a new Access Token.
    *   **API Call:** `POST /auth/api/protected/refresh`
    *   Request Body: `{ "expiredToken": "EXPIRED_ACCESS_TOKEN", "provider": "server" }` (DTO: `TokenRefreshRequest.java`)
    *   The HttpOnly `refreshToken` cookie is automatically sent by the browser.
3.  **Process Token Refresh (Backend - TokenController / TokenService):**
    *   `TokenController.refreshToken()` calls `TokenService.refreshToken()`.
    *   `TokenService`:
        *   Retrieves the Refresh Token from the HttpOnly cookie (via `HttpServletRequest`).
        *   Retrieves the `userId` and `provider` from the (expired) `expiredToken`'s claims using `JwtUtility.getClaimsFromAccessToken()`.
        *   **Validate Refresh Token (Data Read - Redis):** Checks if the Refresh Token from the cookie matches the one stored in Redis for the `userId` and `provider` (`RedisService.isRTokenExist()`). Also validates its signature and expiration using `JwtUtility.validateRefreshJWT()`.
        *   If valid:
            *   **Generate New Access Token:** Calls `JwtUtility.refreshToken()` or `JwtUtility.buildToken()` to create a new Access Token with refreshed expiration, using claims from the old token.
            *   **(Optional) Refresh Token Rotation:** The system might choose to issue a new Refresh Token as well, update it in Redis, and set it in a new cookie. (Current `TokenService` in `JwtUtility` seems to reuse the existing refresh token if valid, but it's a common security practice to rotate them).
4.  **Return New Access Token (Backend -> Client):**
    *   Responds with `200 OK`.
    *   Response Body: Contains the new Access Token (DTO: `TokenRefreshResponse.java`).
    ```json
    {
      "access_token": "NEW_ACCESS_TOKEN"
    }
    ```
    *   Response Headers: May include a new `Set-Cookie` header if the Refresh Token was rotated. The new Access Token is also set in the `Authorization` header of the response.
5.  **Client Updates Access Token:**
    *   Client replaces the old (expired) Access Token with the new one and retries the original failed API request or subsequent requests.

### 5.1.5. 일기 작성 및 AI 감정 분석 요청 흐름 - Diary Creation and AI Analysis Request Flow

1.  **User Writes Diary (Client):**
    *   User is authenticated.
    *   User types their diary title and content into the Client application.
2.  **Submit Diary (Client -> Backend):**
    *   Client sends the diary data to the Backend.
    *   **API Call:** `POST /api/diaries` (New API, assuming it exists as per Chapter 3)
    *   Request Headers: `Authorization: Bearer {access_token}`
    *   Request Body: `{ "title": "My Day", "content": "Today was interesting..." }` (DTO: `DiaryCreateRequest.java`)
3.  **Process Diary Creation (Backend - Application Service):**
    *   `DiaryController` (assumed) receives the request.
    *   Calls `DiaryService.createDiary()` (assumed).
    *   `DiaryService`:
        *   Validates input.
        *   Identifies the user from the JWT's `userId`.
        *   **Data Write (MariaDB):** Creates a `Diary` entity and saves it to the `Diary` table in MariaDB, associating it with the user.
        *   **Publish to Kafka (Async):** After successfully saving the diary, the `DiaryService` (or a dedicated event publisher) constructs a message containing the diary ID and its content (or just ID if AI worker fetches content).
        *   Sends this message to a Kafka topic (e.g., "diary-analysis-requests") using `KafkaTemplate` (similar to `UserEventProducer`).
4.  **Diary Saved Confirmation (Backend -> Client):**
    *   Backend responds to the Client confirming the diary entry has been saved.
    *   Response: `201 Created` with the created Diary object (DTO: `DiaryResponse.java`).
    *   The client UI might indicate "Saved, Analysis in progress..."
5.  **AI Worker Consumes Message (Async - Kafka Consumer & AI Worker):**
    *   The Python AI Worker (a separate service) is subscribed to the "diary-analysis-requests" Kafka topic.
    *   It consumes the message containing the diary ID and content.
6.  **Perform AI Analysis (AI Worker):**
    *   The AI Worker processes the diary content using its NLP/ML models:
        *   Emotion detection.
        *   Automatic thought identification.
        *   Alternative thought suggestion.
7.  **Store Analysis Results (AI Worker -> MongoDB):**
    *   **Data Write (MongoDB):** The AI Worker stores the detailed analysis results in a MongoDB collection (e.g., `diary_analysis_results`), linking it to the `diaryId` and `userId`. The structure would resemble the `DiaryAnalysisResultDto` or the MongoDB schema defined in Chapter 4.
    *   (Optional) The AI worker might send another Kafka message (e.g., to "diary-analysis-completed") if other backend services need to be notified in real-time.

### 5.1.6. 일기 및 분석 결과 조회 흐름 - Diary and Analysis Result Retrieval Flow

1.  **User Requests Diary View (Client):**
    *   User navigates to view a specific diary entry or a list of entries.
    *   For a single entry, client has/requests the `diaryId`.
2.  **Request Diary Data (Client -> Backend):**
    *   **API Call:** `GET /api/diaries/{diaryId}` (API from Chapter 3 / API Docs)
    *   Request Headers: `Authorization: Bearer {access_token}`
3.  **Process Diary Retrieval (Backend - Application Service):**
    *   `DiaryController` (assumed) receives the request.
    *   Calls `DiaryService.getDiaryDetail()` (assumed).
    *   `DiaryService`:
        *   **Data Read (MariaDB):** Fetches the `Diary` entity from MariaDB using `diaryId` via `DiaryRepository`. Verifies user ownership.
        *   **Data Read (MongoDB):** Fetches the corresponding analysis results from the MongoDB `diary_analysis_results` collection using `diaryId` (or `diaryEntryId`). This might be done by the `DiaryService` itself if it has a `MongoTemplate`/`MongoRepository`, or it might call another internal service dedicated to AI results.
        *   Combines the `Diary` entity data and the MongoDB analysis data into a single response DTO (e.g., `DiaryDetailResponse.java`). The `alternativeThought` and `isNegative` fields in the `Diary` entity in MariaDB might store a summary or the primary AI-suggested thought for quick display, while MongoDB holds more detailed analysis.
4.  **Return Combined Data (Backend -> Client):**
    *   Backend responds with `200 OK`.
    *   Response Body: Contains the combined diary and analysis data (DTO: `DiaryDetailResponse.java`).
    ```json
    {
      "id": 123,
      "userId": 1,
      "title": "My Day",
      "content": "Today was interesting...",
      "alternativeThoughtByAI": "Perhaps it was an opportunity for growth.", // From Diary entity
      "isNegative": false, // From Diary entity
      "createdAt": "2023-10-27T10:00:00Z",
      "updatedAt": "2023-10-27T10:00:00Z",
      "analysis": { // From MongoDB, via DiaryAnalysisResultDto
        "id": "mongoAnalysisId123",
        "emotionDetection": "Curiosity 70%, Slight Apprehension 20%",
        "automaticThought": "This might be too hard.",
        "promptForChange": "What makes you say it might be too hard?",
        "alternativeThought": "It's challenging, but I can try to break it down.",
        "status": "NEUTRAL",
        "analyzedAt": "2023-10-27T10:05:00Z"
      }
    }
    ```
5.  **Client Displays Data:**
    *   Client application renders the diary content and its associated emotional analysis for the user.

## 5.2. 주요 상호작용에 대한 시퀀스 다이어그램 (Text-based Descriptions)

### 5.2.1. 비동기 메시지 처리 (Kafka 연동 - 일기 분석 요청) - Asynchronous Message Processing (Kafka for Diary Analysis)

*   **Actors:** `Client`, `API Gateway`, `Application Service (Spring Boot Backend)`, `MariaDB`, `Apache Kafka`, `Python AI Worker`, `MongoDB`.
*   **Flow:**
    1.  `Client` --(HTTP POST `/api/diaries` with diary data & JWT)--> `API Gateway`
    2.  `API Gateway` --(Forward HTTP POST)--> `Application Service (DiaryController)`
    3.  `DiaryController` --> `DiaryService.createDiary(diaryData, userIdFromJwt)`
    4.  `DiaryService` --(Construct `Diary` entity)--> `DiaryRepository.save(diaryEntity)`
    5.  `DiaryRepository` --(SQL INSERT)--> `MariaDB (Diary Table)`
    6.  `MariaDB` --(Return Saved Diary with ID)--> `DiaryRepository`
    7.  `DiaryRepository` --(Return Saved Diary with ID)--> `DiaryService`
    8.  `DiaryService` --(Prepare Kafka message: `{ diaryId, content, userId }`)--> `KafkaProducerService.sendMessage("diary_analysis_requests", kafkaMessage)`
    9.  `KafkaProducerService` --(Publish message)--> `Apache Kafka (Topic: diary_analysis_requests)`
    10. `DiaryService` --(Return `DiaryResponse` DTO)--> `DiaryController`
    11. `DiaryController` --(HTTP 201 Created with `DiaryResponse`)--> `API Gateway`
    12. `API Gateway` --(Forward HTTP 201)--> `Client` (Client UI updated, diary saved)

    --- **Asynchronous Part (Independent Flow)** ---

    13. `Python AI Worker (KafkaConsumer)` --(Polls and receives message from "diary_analysis_requests" topic)--> `Apache Kafka`
    14. `Python AI Worker` --(Deserialize message: `{ diaryId, content, userId }`)
    15. `Python AI Worker` --> `EmotionAnalysisModel.process(content)`
    16. `EmotionAnalysisModel` --(Returns analysis: emotions, thoughts, etc.)--> `Python AI Worker`
    17. `Python AI Worker` --(Constructs analysis document: `{ diaryEntryId, userId, analysisTimestamp, detectedEmotions, ... }`)
    18. `Python AI Worker` --(Save analysis document)--> `MongoDB (diary_analysis_results Collection)`
    19. `MongoDB` --(Acknowledges write)--> `Python AI Worker`
    20. (Optional) `Python AI Worker` might publish a "analysis_completed" event to another Kafka topic if real-time notification to the backend or other services is required.

This textual sequence diagram illustrates the decoupling of diary creation from the AI analysis process, improving responsiveness for the user. The user gets immediate confirmation of diary saving, while the AI processing happens in the background.I have successfully drafted Chapter 5: Main Feature Flowcharts and Sequence Diagrams.

This chapter provides detailed textual descriptions for:
-   **User Scenario based Flowchart Descriptions:**
    -   User Registration (Email Verification Included)
    -   Local Login and JWT Issuance
    -   Social Login (OAuth2) for Kakao, Naver, Google
    -   JWT Token Re-issuance
    -   Diary Creation and AI Emotion Analysis Request (including Kafka interaction)
    -   Diary and Analysis Result Retrieval (combining MariaDB and MongoDB data)
-   **Sequence Diagram (Text-based Description):**
    -   Asynchronous Message Processing (Kafka for Diary Analysis), detailing the steps from client request to AI worker processing and MongoDB storage.

The descriptions include component interactions, API calls (referencing previously defined APIs or assumed ones for Diary/Settings), data R/W operations across MariaDB, Redis, Kafka, and MongoDB, and references to key services and DTOs. It's clearly stated that these are textual descriptions due to the inability to generate visual diagrams.

The content is based on the analysis of existing documentation, service logic, and common architectural patterns for the described features.

The output is a markdown formatted file named `Chapter5_Flows_And_Diagrams.md`.
