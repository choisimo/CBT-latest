# API Interaction and Status Code Handling

This document outlines how the application interacts with the backend API, including how API calls are made, and how responses, especially errors indicated by HTTP status codes, are handled.

## 1. General API Interaction

-   **Primary Mechanism**: Most authenticated API calls are made using the `fetchWithAuth` function provided by `AuthContext.tsx`. This function automatically includes the `Authorization: Bearer <token>` header and `Content-Type: application/json`.
-   **Base URL**: The `BASIC_URL` constant from `CBT-front/src/constants/api.ts` (e.g., `'https://your-api-domain.com'`) is prepended to specific API endpoints.
-   **Unauthenticated Calls**: Some calls, like user registration (`/api/public/join` in `SignupScreen.tsx`), are made using the standard `fetch` API as they do not require authentication.

## 2. Success Handling

-   **Checking for Success**: Successful HTTP responses are generally identified by checking `if (res.ok)` (where `res` is the Response object). This typically corresponds to status codes in the 200-299 range.
-   **Processing Data**: If the request is successful and the response contains a body, the JSON content is parsed using `await res.json()`.
-   **Examples**:
    -   In `SignInScreen.tsx` (via `AuthContext`'s `signIn`), a successful login (`res.ok`) results in parsing `access_token` and `user` from the response.
    -   In `WriteScreen.tsx`, successfully creating a post (`response.ok`) involves parsing `postId` from `responseData`.
    -   In `AnalyzeScreen.tsx`, successfully fetching analysis data means parsing the `data` and then checking its structure to determine if it's an "in-progress" response or a completed "analysis" object.

## 3. Error Handling Strategy

The application employs a multi-layered error handling strategy:

### 3.1. Unauthorized Errors (401) and Token Refresh

-   **Centralized Handling**: The `fetchWithAuth` function in `AuthContext.tsx` has built-in logic to handle `401 Unauthorized` errors.
-   **Token Refresh Mechanism**:
    1.  If an API call made via `fetchWithAuth` returns a `401` status:
    2.  It automatically attempts to refresh the token by making a POST request to `/auth/api/protected/refresh` with the `expiredToken`.
    3.  If the token refresh is successful (`refreshRes.ok`), the new `access_token` is received, stored in Keychain, and the `userToken` state in `AuthContext` is updated.
    4.  The original API request is then retried with the new token.
    5.  If the token refresh fails, user credentials are cleared from Keychain, `userToken` and `user` in `AuthContext` are set to `null` (effectively logging the user out), and the `fetchWithAuth` function returns a `Response` object with a `401` status.
-   **Impact on Components**: This centralized handling means individual components making calls through `fetchWithAuth` often don't need to implement their own 401 handling unless the refresh itself fails, leading to a logout.

### 3.2. Other HTTP Error Status Codes (4xx, 5xx)

-   **Checking `res.ok`**: Most API call sites check `if (!res.ok)` after the fetch operation.
-   **Parsing Error Messages**:
    -   If `!res.ok`, the code typically attempts to parse a JSON error message from the response body: `const errJson = await res.json();` or `const { message } = await res.json();`.
    -   It's generally assumed that the backend will return a JSON object with a `message` field containing a human-readable error.
    -   If parsing fails or `message` is not present, a generic error message is often used (e.g., `'서버 에러가 발생했습니다.'`, `'알 수 없는 오류가 발생했습니다'`, or `err.message` from a caught exception).
-   **Examples**:
    -   **`EmailVerificationScreen.tsx`**:
        ```typescript
        if (!res.ok) {
          const { message } = await res.json();
          throw new Error(message || '인증 실패');
        }
        ```
    -   **`AnalyzeScreen.tsx`**:
        ```typescript
        if (!res.ok) {
          const errJson = await res.json();
          throw new Error(errJson.message || `서버 에러: ${res.status}`);
        }
        ```
    -   **`ViewScreen.tsx`** distinguishes 404 errors:
        ```typescript
        if (!res.ok) {
          if (res.status === 404) {
            throw new Error('해당 글을 찾을 수 없습니다.');
          } else {
            const errJson = await res.json();
            throw new Error(errJson.message || '서버 에러가 발생했습니다.');
          }
        }
        ```
    -   **`WriteScreen.tsx`** (for creating/updating posts):
        ```typescript
        if (!res.ok) { // For PUT
          const errJson = await res.json();
          Alert.alert('수정 실패', errJson.message || '서버 에러가 발생했습니다.');
          return;
        }
        if (!response.ok) { // For POST
          const errorData = await response.json();
          Alert.alert('작성 실패', errorData.message || '서버 에러가 발생했습니다.');
          return;
        }
        ```

### 3.3. Network and Other Generic Errors

-   **`try...catch` Blocks**: Most asynchronous API operations are wrapped in `try...catch (err: any)` blocks.
-   **Catching Errors**: These blocks catch:
    -   Errors thrown manually after checking `!res.ok` (as described above).
    -   Network errors (e.g., if the server is unreachable).
    -   Other unexpected errors during the request/response lifecycle.
-   **User Notification**: The `catch` block typically uses `Alert.alert('오류', err.message)` or a similar generic title to display the error to the user.
    -   `SignupScreen.tsx` example for network error: `Alert.alert('오류', '네트워크 오류가 발생했습니다');`

### 3.4. User Notification of Errors

-   **`Alert.alert()`**: The most common method for notifying users of API errors is `Alert.alert(title, message)` from `react-native`. This displays a native system alert.
-   **On-Screen Messages**: In some cases, errors might be displayed as text directly within the UI. For example, `AnalyzeScreen.tsx` sets an `error` state variable, which is then rendered as `<Text style={styles.errorText}>{error}</Text>`.
-   **Loading States**: `isLoading` or `isAuthLoading` states are used to provide visual feedback during API calls, preventing users from interacting further and indicating that an operation is in progress. Failures often result in setting these loading states to `false`.

## 4. Specific Status Codes/Messages

-   **No Explicitly Defined Frontend Constants**: The codebase does not appear to define its own constants for HTTP status codes (e.g., `const HTTP_STATUS_NOT_FOUND = 404;`). Standard numbers (400, 401, 404, 500) are used directly in checks like `if (res.status === 404)`.
-   **Reliance on Backend Messages**: The application primarily relies on error messages provided by the backend in the JSON response (typically in a `message` field). These messages are then displayed to the user via `Alert.alert`.
-   **Client-Side Validation Messages**: Some screens perform client-side validation before attempting API calls (e.g., `SignInScreen` for email/password format, `SignupScreen` for various field validations, `WriteScreen` for presence of title/content). These use `Alert.alert` with specific, client-defined messages and do not involve HTTP status codes.

In summary, the application uses `fetchWithAuth` for robust authenticated requests with automatic token refresh. Error handling generally involves checking `response.ok`, parsing JSON messages from the backend for non-ok responses, and using `Alert.alert` to inform the user. Specific handling for 401 (token refresh) is centralized in `AuthContext`, while other errors like 404 or generic server errors are handled at the call site.
