# API Documentation

This document provides a comprehensive overview of the REST APIs exposed by the Auth-Server Spring Security application. All APIs are designed to be consumed via JSON.

**Base URL**: `https://oss-emotion.nodove.com/v1.2`
**Authentication**: Bearer Token (JWT) for protected endpoints.
**Response Format**: JSON

---

## 2. Authentication API

### 2.1. Check Authentication Status (Test Only)

*   **Method**: `GET`
*   **Endpoint**: `/auth_check`
*   **Description**: Checks if the current request has a valid authentication token.
*   **Authentication**: Bearer Token required
*   **Response Codes**:
    *   `200`: Authenticated
    *   `401`: Unauthorized

### 2.2. JWT Token Reissue

*   **Method**: `POST`
*   **Endpoint**: `/auth/api/protected/refresh`
*   **Description**: Reissues a new Access Token using an expired Access Token and a valid Refresh Token.
*   **Request Body**:
    ```json
    {
      "expiredToken": "string",
      "provider": "string" // e.g., "server", "google"
    }
    ```
*   **Response Codes**:
    *   `200`: Token reissued successfully
    *   `401`: Refresh Token missing or invalid
    *   `406`: Invalid request or Refresh Token not found in Redis
*   **Response Headers**: `Authorization: Bearer {new_access_token}`
*   **Response Body**:
    ```json
    {
      "access_token": "new_eyJhbGciOiJIUzUxMiJ9..."
    }
    ```

---

## 3. User Management API

### 3.1. User Registration

*   **Method**: `POST`
*   **Endpoint**: `/api/public/join`
*   **Description**: Registers a new user. Email verification code must be validated beforehand.
*   **Request Body**:
    ```json
    {
      "userId": "newUser123",
      "userPw": "password123!",
      "userName": "홍길동",
      "phone": "010-1234-5678",
      "email": "user@example.com",
      "role": "USER", // Default: USER
      "birthDate": "1990-01-01",
      "gender": "male",
      "isPrivate": false,
      "profile": "https://zrr.kr/iPHf", // Profile image URL
      "code": "A1B2C3D4" // Email verification code
    }
    ```
*   **Response Codes**:
    *   `200`: User registration successful
    *   `400`: Bad request
    *   `409`: Conflict (User ID already exists)
    *   `500`: Server error
*   **Response Body**:
    ```json
    {
      "message": "join successfully"
    }
    ```

### 3.2. Login

*   **Method**: `POST`
*   **Endpoint**: `/api/auth/login`
*   **Description**: Logs in a user and issues a JWT token.
*   **Request Body**:
    ```json
    {
      "userId": "newUser123",
      "password": "password123!"
    }
    ```
*   **Response Codes**:
    *   `200`: Login successful
    *   `400`: Bad request
    *   `401`: Login failed
*   **Response Headers**: `Authorization: Bearer {access_token}`, `Set-Cookie: refreshToken=xxxxxx; Path=/; Domain=your-cookie-domain.com; HttpOnly; Secure`
*   **Response Body**:
    ```json
    {
      "access_token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXdVc2VyMTIzIiwicm9sZSI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzE1ODAwMDAwLCJleHAiOjE3MTU4MDE4MDB9.xxxx"
    }
    ```

### 3.3. Profile Image Upload

*   **Method**: `POST`
*   **Endpoint**: `/api/public/profileUpload`
*   **Description**: Uploads a user profile image and returns the image URL.
*   **Request Body**: `multipart/form-data` with `profile` (image file)
*   **Response Codes**:
    *   `200`: Upload successful
    *   `400`: Invalid file
    *   `500`: Server error
*   **Response Body**:
    ```json
    {
      "fileName": "https://your-file-server.com/attach/profile/xxxx_profile.jpg"
    }
    ```

### 3.4. User ID Duplicate Check

*   **Method**: `POST`
*   **Endpoint**: `/api/public/check/userId/IsDuplicate`
*   **Description**: Checks if the provided user ID is already in use.
*   **Request Body**:
    ```json
    {
      "userId": "newUser123"
    }
    ```
*   **Response Codes**:
    *   `200`: Duplicate check result
*   **Response Body**: `boolean` (true: duplicate, false: available)

### 3.5. Nickname Duplicate Check

*   **Method**: `POST`
*   **Endpoint**: `/api/public/check/nickname/IsDuplicate`
*   **Description**: Checks if the provided nickname is already in use.
*   **Request Body**:
    ```json
    {
      "nickname": "쾌활한다람쥐"
    }
    ```
*   **Response Codes**:
    *   `200`: Duplicate check result
*   **Response Body**: `boolean` (true: duplicate, false: available)

### 3.6. Clean User Token Cookie (Logout)

*   **Method**: `POST`
*   **Endpoint**: `/api/public/clean/userTokenCookie`
*   **Description**: Expires and removes the client's refreshToken cookie.
*   **Response Codes**:
    *   `200`: Cookie cleaned successfully
*   **Response Body**:
    ```json
    {
      "message": "User token cookie cleaned successfully"
    }
    ```

---

## 5. Email API

### 5.1. Send Email Verification Code

*   **Method**: `POST`
*   **Endpoint**: `/api/public/emailSend`
*   **Description**: Sends an email verification code for registration.
*   **Request Body**:
    ```json
    {
      "email": "user@example.com"
    }
    ```
*   **Response Codes**:
    *   `200`: Email sent successfully
    *   `400`: Email already registered
    *   `500`: Email sending failed
*   **Response Body**:
    ```json
    {
      "message": "A temporary code has been sent to your email"
    }
    ```

### 5.2. Send Custom Email (Admin/Internal Use)

*   **Method**: `POST`
*   **Endpoint**: `/api/private/customEmailSend`
*   **Description**: Sends a custom email with a specified subject and content to a recipient.
*   **Authentication**: Bearer Token required
*   **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "content": "이메일 내용",
      "title": "이메일 제목"
    }
    ```
*   **Response Codes**:
    *   `200`: Email sent successfully
    *   `400`: Bad request
    *   `500`: Email sending failed
*   **Response Body**:
    ```json
    {
      "message": "custom email send success"
    }
    ```

### 5.3. Verify Email Verification Code

*   **Method**: `POST`
*   **Endpoint**: `/api/public/emailCheck`
*   **Description**: Verifies the validity of a sent email verification code.
*   **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "code": "A1B2C3D4"
    }
    ```
*   **Response Codes**:
    *   `202`: Email code valid
    *   `401`: Email code invalid
*   **Response Body**:
    ```json
    {
      "message": "email code is valid"
    }
    ```

### 5.4. Send Temporary Password Email (Authenticated User)

*   **Method**: `POST`
*   **Endpoint**: `/api/protected/sendEmailPassword`
*   **Description**: Sends a temporary password to the currently logged-in user's email.
*   **Authentication**: Bearer Token required
*   **Response Codes**:
    *   `200`: Temporary password sent successfully
    *   `500`: Temporary password sending failed
*   **Response Body**:
    ```json
    {
      "message": "A temporary password has been sent to your email"
    }
    ```

### 5.5. Find Password by User ID and Send Temporary Password

*   **Method**: `GET`
*   **Endpoint**: `/api/public/findPassWithEmail`
*   **Description**: Finds the registered email based on user ID and sends a temporary password.
*   **Request Body**:
    ```json
    {
      "userId": "newUser123"
    }
    ```
*   **Response Codes**:
    *   `200`: Temporary password sent successfully
    *   `500`: Temporary password sending failed
*   **Response Body**:
    ```json
    {
      "message": "A temporary password has been sent to your email"
    }

    ```

---

## 6. SSE API

### 6.1. SSE (Server-Sent Events) Subscription

*   **Method**: `GET`
*   **Endpoint**: `/api/protected/sse/subscribe`
*   **Description**: Subscribes to a real-time event stream from the server.
*   **Authentication**: Bearer Token required
*   **Request Headers**: `Last-Event-ID`: Last received event ID (optional)
*   **Response Codes**:
    *   `200`: SSE subscription successful
    *   `401`: Authentication failed
    *   `500`: Server error
*   **Response Format**: `text/event-stream`
*   **Response Example**:
    ```text
    id: 123
    event: INIT
    data: {"message": "Subscription successful"}

    id: 124
    event: message
    data: {"content": "New notification!"}
    ```

### 6.2. Send Dummy SSE Data to Specific User (Test Only)

*   **Method**: `POST`
*   **Endpoint**: `/api/public/dummyData/{user_id}`
*   **Description**: Triggers an SSE event for a specified user ID.
*   **Path Parameters**: `user_id`: ID of the user to receive the SSE event
*   **Request Body**:
    ```json
    {
      "message": "Hello from server!"
    }
    ```
*   **Response Codes**:
    *   `200`: Dummy data sent successfully
    *   `400`: Bad request

---

## 7. Diary API

### 7.1. Create Diary

*   **Method**: `POST`
*   **Endpoint**: `/api/diaries`
*   **Description**: Creates and saves a new diary entry.
*   **Authentication**: Bearer Token required
*   **Request Body**:
    ```json
    {
      "title": "오늘의 일기", // Optional
      "content": "오늘은 정말 즐거운 하루였다." // Required
    }
    ```
*   **Response Codes**:
    *   `201`: Diary created successfully
    *   `400`: Bad request
    *   `401`: Authentication failed
*   **Response Body**:
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

### 7.2. Get Diary List

*   **Method**: `GET`
*   **Endpoint**: `/api/diaries`
*   **Description**: Retrieves a paginated list of diary entries written by the user.
*   **Authentication**: Bearer Token required
*   **Query Parameters**: `page`: Page number (default 0), `size`: Items per page (default 10), `sort`: Sort criteria (e.g., `createdAt,desc`)
*   **Response Codes**:
    *   `200`: Diary list retrieved successfully
    *   `401`: Authentication failed
*   **Response Body**:
    ```json
    {
      "diaries": [
        {
          "id": 1,
          "title": "오늘의 일기",
          "createdAt": "2023-05-01T12:00:00Z",
          "emotionStatus": "POSITIVE"
        }
        // ... additional diary entries
      ],
      "pageInfo": {
        "currentPage": 0,
        "totalPages": 5,
        "totalElements": 42
      }
    }
    ```

### 7.3. Get Specific Diary Details

*   **Method**: `GET`
*   **Endpoint**: `/api/diaries/{diaryId}`
*   **Description**: Retrieves the detailed content of a specific diary entry by ID, along with associated emotion analysis results.
*   **Authentication**: Bearer Token required
*   **Path Parameters**: `diaryId`: ID of the diary to retrieve
*   **Response Codes**:
    *   `200`: Diary details retrieved successfully
    *   `401`: Authentication failed
    *   `403`: Access denied
    *   `404`: Diary not found
*   **Response Body**:
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

### 7.4. Update Specific Diary

*   **Method**: `PUT`
*   **Endpoint**: `/api/diaries/{diaryId}`
*   **Description**: Modifies the title or content of a specific diary entry by ID.
*   **Authentication**: Bearer Token required
*   **Path Parameters**: `diaryId`: ID of the diary to update
*   **Request Body**:
    ```json
    {
      "title": "수정된 일기 제목", // Optional
      "content": "수정된 일기 내용" // Required
    }
    ```
*   **Response Codes**:
    *   `200`: Diary updated successfully
    *   `400`: Bad request
    *   `401`: Authentication failed
    *   `403`: Access denied
    *   `404`: Diary not found
*   **Response Body**:
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

### 7.5. Delete Specific Diary

*   **Method**: `DELETE`
*   **Endpoint**: `/api/diaries/{diaryId}`
*   **Description**: Deletes a specific diary entry by ID.
*   **Authentication**: Bearer Token required
*   **Path Parameters**: `diaryId`: ID of the diary to delete
*   **Response Codes**:
    *   `204`: Diary deleted successfully (No Content)
    *   `401`: Authentication failed
    *   `403`: Access denied
    *   `404`: Diary not found

---

## 9. Settings API

### 9.1. Get User Settings

*   **Method**: `GET`
*   **Endpoint**: `/api/settings`
*   **Description**: Retrieves the current user's custom settings and default application settings.
*   **Authentication**: Bearer Token required
*   **Response Codes**:
    *   `200`: Settings retrieved successfully
    *   `401`: Authentication failed
*   **Response Body**:
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
        // ... additional settings
      ]
    }
    ```

### 9.2. Update User Settings

*   **Method**: `PUT`
*   **Endpoint**: `/api/settings`
*   **Description**: Modifies specific settings for the currently logged-in user.
*   **Authentication**: Bearer Token required
*   **Request Body**:
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
        // ... additional settings to update
      ]
    }
    ```
*   **Response Codes**:
    *   `200`: Settings updated successfully
    *   `400`: Bad request
    *   `401`: Authentication failed
*   **Response Body**:
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
