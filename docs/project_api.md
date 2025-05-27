### **Project API Specification (project_api.md)**

This document provides a high-level overview of the API endpoints for the Emotion Diary project.

#### **1. Authentication & Authorization (`/auth`, `/oauth2`)**

*   **1.1. Native Login**
    *   **Endpoint:** `POST /auth/login`
    *   **Description:** Authenticates a user with ID and password.
    *   **Request Body (example):** `{ "username": "testuser", "password": "password123" }`
    *   **Response Body (example):** `TokenDto` (containing Access and Refresh Tokens)
        *   `{ "grantType": "Bearer", "accessToken": "...", "refreshToken": "..." }`
    *   **Associated Components:** `AuthController`, `AuthenticationFilter`, `UserService`, `JwtUtility`

*   **1.2. Social Login (OAuth2)**
    *   **Initiation Endpoint:** `GET /oauth2/authorization/{provider}` (e.g., `/oauth2/authorization/google`)
        *   **Description:** Redirects the user to the social provider's authentication page.
    *   **Callback Endpoint:** `GET /auth/login/oauth2/code/{provider}`
        *   **Description:** Handles the callback from the social provider after successful authentication. Receives an authorization code.
        *   **Request Parameters:** `code` (from provider)
        *   **Response Body (example):** `OAuth2LoginResponse` (containing service JWT, user info, and whether the user is new)
            *   `{ "accessToken": "...", "refreshToken": "...", "user": { ... }, "isNewUser": true/false }`
    *   **Associated Components:** `Oauth2Controller`, `Oauth2Service`, `UserService`, `JwtUtility`

*   **1.3. Token Management (Example: Refresh)**
    *   **Endpoint:** (Specific endpoint not detailed, but likely `POST /auth/refresh` or similar)
    *   **Description:** Allows refreshing an expired Access Token using a valid Refresh Token.
    *   **Request Body (example):** `{ "refreshToken": "..." }`
    *   **Response Body (example):** New `TokenDto`
    *   **Associated Components:** `TokenController`, `TokenService`, `JwtUtility`, Redis for Refresh Token storage.

#### **2. User Management (`/users`)**

*   **2.1. User Registration (Sign-up)**
    *   **Endpoint:** (Likely `POST /users/signup` or `POST /auth/register`)
    *   **Description:** Creates a new user account.
    *   **Request Body (example):** User details (e.g., email, password, name).
    *   **Response Body (example):** User information or success message.
    *   **Associated Components:** `UsersController`, `UserService`

*   **2.2. User Profile**
    *   **Endpoint:** (Likely `GET /users/me` or `GET /users/{userId}/profile`)
    *   **Description:** Retrieves the profile information of the authenticated user or a specific user.
    *   **Response Body (example):** User details (excluding sensitive information if for another user).
    *   **Associated Components:** `UsersController`, `UserService`

*   **2.3. User Statistics**
    *   **Endpoint:** (Specific endpoint not detailed, likely admin-only or for the user themselves, e.g., `GET /users/me/stats`)
    *   **Description:** Retrieves usage statistics for a user.
    *   **Associated Components:** `UsersController`, `UserService`

#### **3. Email Service (`/email`)**

*   **Endpoint:** (Specific endpoints not detailed, likely internal or admin-triggered, e.g., `POST /email/send-verification`)
*   **Description:** Handles sending emails for various purposes like account verification, password reset, notifications.
*   **Associated Components:** `EmailController`, `smtpConfig`.
*   **Note:** Email sending is often triggered by other services (e.g., User Service during registration) rather than direct client API calls for all functions.

#### **4. Server-Sent Events (SSE) (`/sse`)**

*   **Endpoint:** (Likely `GET /sse/connect` or `GET /sse/notifications`)
*   **Description:** Establishes a Server-Sent Events connection for real-time updates (e.g., notifications, alerts).
*   **Response:** A stream of events.
*   **Associated Components:** `SseController`, `SseService`.

#### **5. Diary Management (`/diaries`)**

*   **5.1. Create Diary Entry**
    *   **Endpoint:** (Likely `POST /diaries`)
    *   **Description:** Creates a new diary entry.
    *   **Request Body (example):** `DiaryCreateRequest` (content, emotion, etc.)
    *   **Response Body (example):** `DiaryDetailResponse` or diary ID.
    *   **Associated Components:** (Controller inferred), `DiaryCreateRequest`, `DiaryDetailResponse`.

*   **5.2. View Diary Entry**
    *   **Endpoint:** (Likely `GET /diaries/{diaryId}`)
    *   **Description:** Retrieves a specific diary entry.
    *   **Response Body (example):** `DiaryDetailResponse`.
    *   **Associated Components:** (Controller inferred), `DiaryDetailResponse`.

*   **5.3. Request Diary Analysis**
    *   **Endpoint:** (Likely `POST /diaries/{diaryId}/analyze`)
    *   **Description:** Triggers an analysis of the diary entry.
    *   **Associated Components:** (Controller inferred).

#### **6. Report Management (`/reports`)**

*   **Endpoint:** (Likely `GET /reports` or `GET /reports/{reportId}` or `GET /diaries/{diaryId}/report`)
*   **Description:** Manages and retrieves reports generated from diary analysis.
*   **Associated Components:** (`Report` domain, controller inferred).

#### **7. Admin Features (`/admin`)**

*   **7.1. Dynamic Filter Management**
    *   **Endpoint:** (Specific endpoints not detailed, e.g., `GET /admin/filters`, `POST /admin/filters`, `DELETE /admin/filters/{filterId}`)
    *   **Description:** Allows administrators to dynamically manage security filter conditions.
    *   **Associated Components:** `AdminFilterController`.

