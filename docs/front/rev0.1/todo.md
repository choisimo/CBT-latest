# Frontend-Backend Integration TODOs and Status

This document outlines the connection status, completion status, and identified gaps between the frontend features (as documented and implemented) and the backend API (as documented in `docs/auth-server/rev2.1/API_Documentation.md` and `Auth-server/backend/docs/API_Documentation.md`).

---

## User Authentication

### 1. User Sign-In (Login)

-   **Feature Name:** User Authentication - Sign In
-   **Frontend Implementation Status:** Complete
-   **Backend API Endpoint(s):**
    -   `POST /api/auth/login` (for username/password login)
    -   `POST /auth/api/protected/refresh` (for token refresh)
    -   `GET /api/users/me` (implicitly used by `AuthContext` bootstrap and `refreshUser` to get user details after login/token refresh)
-   **Connection/Integration Notes:**
    -   Successfully integrates with `/api/auth/login` via `AuthContext`'s `signIn` function.
    -   Token (access_token) and user details are stored in context.
    -   Token is stored in Keychain.
    -   Automatic token refresh on 401 errors is handled by `fetchWithAuth` in `AuthContext` using `/auth/api/protected/refresh`.
    -   Error messages from the backend (e.g., invalid credentials) are displayed using `Alert.alert`.
-   **Missing/Unimplemented (Frontend):**
    -   "Forgot Password" or "Find ID" functionality is not implemented on the Sign In screen, though backend has `/api/public/findPassWithEmail`.
-   **Missing/Unimplemented (Backend Support):** None identified for core sign-in.
-   **General Notes/Todos:**
    -   The `AuthContext` `signIn` function expects `userId` and `password`. The backend docs for `/api/auth/login` also specify `userId` and `password`.
    -   The frontend `User` type in `AuthContext` includes `userId`, `nickname`, `role`, `emailVerified`. The backend login response (`/api/auth/login`) in `AuthContext` is destructured as `{ access_token, user }`, implying the backend sends the user object directly upon login. This matches the frontend expectation.

### 2. User Sign-Up (Registration)

-   **Feature Name:** User Authentication - Sign Up
-   **Frontend Implementation Status:** Complete (but relies on prior email verification not explicitly part of this screen's direct API call)
-   **Backend API Endpoint(s):**
    -   `POST /api/public/join`
-   **Connection/Integration Notes:**
    -   `SignupScreen.tsx` directly calls `/api/public/join`.
    -   It sends `email`, `userPw` (password), `userName`, `nickname`.
    -   The backend docs for `/api/public/join` list more fields in `JoinRequest` (`phone`, `role`, `birthDate`, `gender`, `isPrivate`, `profile`, `code`). The frontend only sends a subset. The backend probably has defaults or handles missing optional fields.
    -   The `code` field (email verification code) is mentioned as required in the backend doc description ("이메일 인증 코드가 사전에 검증되어야 합니다") but not explicitly shown as sent by the current frontend `SignupScreen.tsx`'s `handleSignup` function. The frontend `EmailVerificationScreen.tsx` handles code verification separately using `/api/auth/verify-email` (which doesn't directly align with `/api/public/emailCheck` from backend docs for verification, see feature 3). This implies a two-step process: verify email first, then sign up.
    -   Error messages (e.g., duplicate ID/nickname, validation) from the backend are displayed using `Alert.alert`.
-   **Missing/Unimplemented (Frontend):**
    -   Frontend does not send `phone`, `birthDate`, `gender`, `isPrivate`, `profile` during sign-up. It's unclear if these are intended to be optional or collected later.
    -   No UI for ID/Nickname duplicate checks before submission, though backend provides `/api/public/check/userId/IsDuplicate` and `/api/public/check/nickname/IsDuplicate`.
-   **Missing/Unimplemented (Backend Support):** None identified for core sign-up with basic fields.
-   **General Notes/Todos:**
    -   Clarify if the `code` in `JoinRequest` for `/api/public/join` is still necessary if email verification is a separate step. The current frontend flow suggests email verification happens *before* actual sign-up.
    -   Consider adding client-side calls to duplicate check APIs for better UX.

### 3. Email Verification

-   **Feature Name:** User Authentication - Email Verification
-   **Frontend Implementation Status:** Complete
-   **Backend API Endpoint(s):**
    -   Frontend uses: `POST /api/auth/verify-email` (from `EmailVerificationScreen.tsx`)
    -   Backend docs list:
        -   `POST /api/public/emailSend` (to send verification code)
        -   `POST /api/public/emailCheck` (to verify code)
-   **Connection/Integration Notes:**
    -   `EmailVerificationScreen.tsx` calls `fetchWithAuth` with `/api/auth/verify-email` and the `code`.
    -   After successful verification, `refreshUser()` is called to update user state (presumably `emailVerified` flag).
    -   Backend `rev2.1` doc lists `/api/public/emailSend` and `/api/public/emailCheck`. The frontend's `/api/auth/verify-email` is not explicitly in `rev2.1` but might be an endpoint from an older version or a specific detail of the "CBT" project not in the generic auth server docs. The `original_doc` does not list `/api/auth/verify-email` either.
    -   The `EmailVerificationScreen` does not handle sending the email itself; it assumes the code has already been sent (likely triggered after signup).
-   **Missing/Unimplemented (Frontend):**
    -   No frontend UI to trigger `/api/public/emailSend` if the user didn't receive the code or it expired.
-   **Missing/Unimplemented (Backend Support):** The endpoint `/api/auth/verify-email` used by the frontend is not clearly documented in the provided backend API docs (rev2.1 or original). The documented endpoint is `/api/public/emailCheck`. This needs clarification.
-   **General Notes/Todos:**
    -   Verify the correct backend endpoint for email verification (`/api/auth/verify-email` vs `/api/public/emailCheck`).
    -   The `AuthStack.tsx` has logic to show `VerifyEmail` screen if `user && !user.emailVerified`. This relies on the `user.emailVerified` flag being correctly set by the backend and fetched by `refreshUser()`.

### 4. User Sign-Out (Logout)

-   **Feature Name:** User Authentication - Sign Out
-   **Frontend Implementation Status:** Complete
-   **Backend API Endpoint(s):**
    -   `POST /api/auth/logout` (used by `AuthContext`)
    -   Backend doc also lists `POST /api/public/clean/userTokenCookie` (for soft logout, not used by frontend `AuthContext`)
-   **Connection/Integration Notes:**
    -   `AuthContext`'s `signOut` function calls `fetchWithAuth` with `/api/auth/logout`.
    -   Keychain is reset, and local `userToken` and `user` state are cleared.
-   **Missing/Unimplemented (Frontend):** None for basic logout.
-   **Missing/Unimplemented (Backend Support):** None for basic logout.
-   **General Notes/Todos:** The frontend implements a full logout. The backend's `/api/public/clean/userTokenCookie` for just clearing cookies is not used.

---

## Diary Management

### 1. Create New Diary Entry

-   **Feature Name:** Diary - Create Entry
-   **Frontend Implementation Status:** Complete
-   **Backend API Endpoint(s):**
    -   Frontend uses: `POST /api/diaryposts` (from `WriteScreen.tsx`)
    -   Backend docs (`rev2.1` and `original_doc`): `POST /api/diaries`
-   **Connection/Integration Notes:**
    -   `WriteScreen.tsx` (in create mode) calls `fetchWithAuth` to `https://<BASIC_URL>/api/diaryposts` with `date`, `title`, `content`.
    -   Backend docs specify `POST /api/diaries` with `title` (optional) and `content` (required). The frontend sends `date` as well.
    -   The frontend navigates to `AnalyzeScreen` with the new `postId` from the response.
    -   Field discrepancy: Frontend sends `date`, backend `DiaryCreateRequest` in `rev2.1` does not list `date`. `original_doc`'s `DiaryCreateRequest` also only lists `title` and `content`. This might be a mismatch or the backend handles the extra `date` field gracefully.
-   **Missing/Unimplemented (Frontend):** None for basic creation.
-   **Missing/Unimplemented (Backend Support):** Clarify if `/api/diaries` accepts a `date` field or if the frontend should only send `title` and `content`. The endpoint path also differs (`/api/diaryposts` vs `/api/diaries`).
-   **General Notes/Todos:** Critical mismatch in endpoint path (`/api/diaryposts` vs `/api/diaries`) and potentially request body (`date` field). This needs immediate clarification.

### 2. View Diary Entries (List)

-   **Feature Name:** Diary - View List & Calendar
-   **Frontend Implementation Status:** Complete
-   **Backend API Endpoint(s):**
    -   Frontend uses: `GET /api/diaries` (from `MainScreen.tsx` for fetching posts and dates for calendar)
    -   Backend docs (`rev2.1` and `original_doc`): `GET /api/diaries` (for list)
-   **Connection/Integration Notes:**
    -   `MainScreen.tsx` uses `fetchWithAuth` to get a list of posts and dates. Supports pagination (`page`, `size`), sorting (`sort`), and `searchText`. Also uses `startDate`, `endDate` for date-specific filtering.
    -   The backend `GET /api/diaries` endpoint in docs supports pagination and sorting. The response structure in docs (`diaries` list and `pageInfo`) matches general frontend expectations.
    -   `MainScreen.tsx` expects `dates` array in the response for `PostData` when loading dates for the calendar. The backend API doc for `GET /api/diaries` in `rev2.1` shows a response with `diaries` (list of `DiaryListItem`) and `pageInfo`. It does not explicitly mention a top-level `dates` array. `original_doc` is similar. This is a potential mismatch.
-   **Missing/Unimplemented (Frontend):** None apparent for basic list viewing.
-   **Missing/Unimplemented (Backend Support):** The `dates` array expected by `MainScreen.tsx` in the response of `GET /api/diaries` is not documented in the backend API.
-   **General Notes/Todos:** Clarify the response structure of `GET /api/diaries`, specifically the presence of the `dates` array for calendar marking.

### 3. View Single Diary Entry (Details)

-   **Feature Name:** Diary - View Entry Details
-   **Frontend Implementation Status:** Complete
-   **Backend API Endpoint(s):**
    -   Frontend uses: `GET /api/diaryposts/{postId}` (from `ViewScreen.tsx`)
    -   Backend docs (`rev2.1` and `original_doc`): `GET /api/diaries/{diaryId}`
-   **Connection/Integration Notes:**
    -   `ViewScreen.tsx` uses `fetchWithAuth` to get details for a `postId`.
    -   Expects `id`, `date`, `title`, `content`, `aiResponse` (boolean indicating if analysis exists).
    -   Backend `GET /api/diaries/{diaryId}` in `rev2.1` doc returns `id`, `userId`, `title`, `content`, `alternativeThoughtByAI`, `createdAt`, `updatedAt`, and an `analysis` object.
    -   The `original_doc` for `GET /api/diaries/{diaryId}` is more aligned with frontend expectation of an `analysis` object, which would imply `aiResponse` can be derived.
-   **Missing/Unimplemented (Frontend):** Frontend does not display all fields from backend (e.g., `alternativeThoughtByAI` directly on this screen, `userId`, `createdAt`, `updatedAt` from the main diary object).
-   **Missing/Unimplemented (Backend Support):** Endpoint path mismatch (`/api/diaryposts/{postId}` vs `/api/diaries/{diaryId}`). The structure for `aiResponse` flag needs to be confirmed against the `analysis` object from backend.
-   **General Notes/Todos:** Endpoint path mismatch. Frontend should derive `aiResponse` from the presence/status of the `analysis` object if the backend sends the detailed analysis object.

### 4. Edit Diary Entry

-   **Feature Name:** Diary - Edit Entry
-   **Frontend Implementation Status:** Complete
-   **Backend API Endpoint(s):**
    -   Fetch existing: `GET /api/diaryposts/{postId}` (from `WriteScreen.tsx`)
    -   Update: `PUT /api/diaryposts/{postId}` (from `WriteScreen.tsx`)
    -   Backend docs (`rev2.1` and `original_doc`):
        -   Fetch existing: `GET /api/diaries/{diaryId}`
        -   Update: `PUT /api/diaries/{diaryId}`
-   **Connection/Integration Notes:**
    -   `WriteScreen.tsx` (in edit mode) fetches existing data.
    -   Sends `title`, `content` for update. Backend `DiaryUpdateRequest` also expects `title` (optional), `content` (required).
    -   Path mismatch for both GET and PUT operations.
-   **Missing/Unimplemented (Frontend):** Frontend does not allow editing the `date` of an existing post, though it's fetched. The PUT request only sends `title` and `content`.
-   **Missing/Unimplemented (Backend Support):** Endpoint path mismatch.
-   **General Notes/Todos:** Clarify endpoint paths. Backend API for PUT does not mention updating the `date`.

### 5. Delete Diary Entry

-   **Feature Name:** Diary - Delete Entry
-   **Frontend Implementation Status:** Not Started (No UI element or function call observed for deleting entries)
-   **Backend API Endpoint(s):**
    -   `DELETE /api/diaries/{diaryId}`
-   **Connection/Integration Notes:** -
-   **Missing/Unimplemented (Frontend):** Entire feature.
-   **Missing/Unimplemented (Backend Support):** None (endpoint exists).
-   **General Notes/Todos:** This is a potential future feature.

### 6. AI Analysis of Diary Entry

-   **Feature Name:** Diary - AI Analysis
-   **Frontend Implementation Status:** Complete (for viewing and initiating)
-   **Backend API Endpoint(s):**
    -   Get Analysis / Check Status: `GET /api/diaries/{postId}/analysis` (from `AnalyzeScreen.tsx`)
    -   Request Analysis: `POST /api/diaries/{postId}/analysis` (from `ViewScreen.tsx` if `aiResponse` is false)
    -   Backend docs (`rev2.1` and `original_doc`): The Diary API section in `rev2.1` does not explicitly list `/analysis` sub-resource endpoints. However, `DiaryDetailResponse` includes an `analysis` object. The `original_doc` (section 6.3) for `GET /api/diaries/{diaryId}` includes the `analysis` object. It does not explicitly list an endpoint to *request* analysis if not present. The `AnalyzeScreen.tsx` also mentions a response structure for "analysis in progress" (`message`, `progress`, `estimatedRemaining`), which isn't detailed in the backend API docs for diary analysis.
-   **Connection/Integration Notes:**
    -   `AnalyzeScreen.tsx` fetches analysis results. It handles responses indicating "analysis in progress" or "analysis complete".
    -   `ViewScreen.tsx` can trigger a POST request to `/api/diaries/{postId}/analysis` to initiate analysis.
-   **Missing/Unimplemented (Frontend):** None for basic viewing/requesting.
-   **Missing/Unimplemented (Backend Support):**
    -   The backend docs for Diary API (both `rev2.1` and `original_doc`) do not clearly define the `GET /api/diaries/{postId}/analysis` for polling status or the `POST /api/diaries/{postId}/analysis` for initiating analysis. The `DiaryDetailResponse` implies analysis is part of the diary details, but the frontend actively polls/requests it separately.
    -   The "analysis in progress" response structure (`message`, `progress`, `estimatedRemaining`) is not documented.
-   **General Notes/Todos:** This area has significant discrepancies between frontend implementation and backend documentation. The frontend seems to expect more specific endpoints and response types for handling asynchronous analysis than what is documented.

---

## Other Features

### 1. User Profile Management

-   **Feature Name:** User Profile Management
-   **Frontend Implementation Status:** Not Started (No dedicated screen or UI elements observed for viewing/editing user profile details like name, nickname, phone, profile image etc., beyond what's entered at sign-up)
-   **Backend API Endpoint(s):**
    -   `POST /api/public/profileUpload` (for uploading profile image)
    -   (Potentially an endpoint like `PUT /api/users/me` or `PUT /api/protected/user/update` would be needed for updating user details, but not explicitly found in a way that frontend `AuthContext.User` type would map to for update).
-   **Connection/Integration Notes:** -
-   **Missing/Unimplemented (Frontend):** Entire feature.
-   **Missing/Unimplemented (Backend Support):** While profile image upload exists, a clear endpoint for updating other user details (nickname, name etc.) is not obvious or seems missing.
-   **General Notes/Todos:** Potential future feature.

### 2. Settings Management

-   **Feature Name:** Application/User Settings
-   **Frontend Implementation Status:** Not Started
-   **Backend API Endpoint(s):**
    -   `GET /api/settings`
    -   `PUT /api/settings`
-   **Connection/Integration Notes:** -
-   **Missing/Unimplemented (Frontend):** Entire feature.
-   **Missing/Unimplemented (Backend Support):** None (endpoints exist).
-   **General Notes/Todos:** Potential future feature.

### 3. Password Management

-   **Feature Name:** Password Management (Change password, Forgot password)
-   **Frontend Implementation Status:** Not Started
-   **Backend API Endpoint(s):**
    -   `POST /api/protected/sendEmailPassword` (Send temporary password to logged-in user's email)
    -   `GET /api/public/findPassWithEmail` (Send temporary password to email found via userId)
    -   (An endpoint to change password using current and new password would be typical, e.g., `PUT /api/protected/user/change-password`, but not explicitly found).
-   **Connection/Integration Notes:** -
-   **Missing/Unimplemented (Frontend):** Entire feature.
-   **Missing/Unimplemented (Backend Support):** A dedicated "change password" endpoint (current + new password) seems to be missing.
-   **General Notes/Todos:** Potential future feature.

---

## Overall Backend Integration Status & Key Issues

-   **Core Authentication:** Largely complete and functional, including token refresh.
    -   **Issue**: Discrepancy in email verification endpoint (`/api/auth/verify-email` vs `/api/public/emailCheck`).
-   **Diary CRUD:**
    -   **Critical Issue**: Mismatch in base endpoint path for diary operations. Frontend uses `/api/diaryposts`, backend documents `/api/diaries`. This needs immediate resolution.
    -   **Issue**: Frontend sends `date` field when creating diary, not documented in backend request model.
    -   **Issue**: `MainScreen.tsx` expects a `dates` array in `GET /api/diaries` response, which is not in backend docs.
    -   **Issue**: Delete diary feature not implemented in frontend.
-   **AI Analysis:**
    -   **Critical Issue**: Endpoints for initiating (`POST`) and polling/getting (`GET`) AI analysis results (`/api/diaries/{postId}/analysis`) and the "in-progress" response structure are used by frontend but not clearly documented by backend.
-   **Unimplemented Features:** User Profile Management, Settings, comprehensive Password Management are not implemented in frontend, though some backend support exists.
-   **OAuth Logins:** Backend has extensive OAuth2 endpoints (`/api/public/oauth2/login_url/{provider}`, `/api/public/oauth2/callback/{provider}`, and specific `/oauth2/callback/{provider}` POST endpoints). Frontend does not currently use these; standard login is implemented. This is a major unimplemented area if social logins are desired.

## Potential Future Work / Unclear Areas

1.  **OAuth Integration:** Implement frontend logic to use the backend's OAuth2 endpoints.
2.  **User Profile Screen:** Create UI for users to view/update their profiles (name, nickname, avatar via `/api/public/profileUpload`). Needs a backend endpoint to update text-based profile info.
3.  **Settings Screen:** Implement UI for user settings, integrating with `GET/PUT /api/settings`.
4.  **Password Management UI:** Implement "Forgot Password" (using `/api/public/findPassWithEmail`) and "Change Password" (needs new backend endpoint).
5.  **Diary Deletion:** Add UI for deleting diary entries.
6.  **Resolve API Endpoint Mismatches:** Align frontend and backend for diary paths and `/api/auth/verify-email`.
7.  **Clarify AI Analysis Flow:** Document and align the asynchronous AI analysis request/poll/retrieve flow between frontend and backend.
8.  **SSE Notifications:** Backend has `/api/protected/sse/subscribe`. Frontend does not seem to use this. Could be for real-time notifications.
9.  **Admin Features:** Backend has "Admin Filter Management API". No corresponding frontend. (Likely out of scope for user-facing app).
10. **RootNavigator Logic:** The logic for switching between AuthStack and AppStack based on `user.emailVerified` in `RootNavigator.tsx` is currently commented out. This should be reviewed and enabled if email verification is a strict requirement before accessing app features.

---
