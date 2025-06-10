# State Management and Lifecycle

This document outlines the global state management approach using React Context, specifically `AuthContext`, and briefly touches upon local component state.

## Global State: `AuthContext`

The primary mechanism for managing global application state, particularly user authentication and session information, is `AuthContext`.

### 1. AuthContext Overview

-   **Path**: `CBT-front/src/context/AuthContext.tsx`
-   **Purpose**: `AuthContext` provides a way to share authentication-related data and functions across the component tree without passing props down manually at every level. It handles user authentication, session persistence (via Keychain), and provides a centralized way to make authenticated API calls.

### 2. State Structure

The context manages the following pieces of state:

-   **`userToken`**:
    -   **Type**: `string | null`
    -   **Description**: Stores the JWT access token for the authenticated user. It's `null` if no user is logged in. This token is used in the `Authorization` header for authenticated API requests.
-   **`user`**:
    -   **Type**: `User | null`
        ```typescript
        export type User = {
          userId: string;
          nickname: string;
          role: string;
          emailVerified: boolean; // Email verification status
        };
        ```
    -   **Description**: Stores an object containing information about the currently logged-in user (ID, nickname, role, email verification status). It's `null` if no user is logged in.
-   **`isBootstrapping`**:
    -   **Type**: `boolean`
    -   **Description**: Indicates if the application is currently in the process of checking for existing credentials (e.g., on app startup). It's `true` while trying to retrieve a token from Keychain and fetch initial user data, and `false` otherwise. This is useful for showing a loading screen when the app first launches.
-   **`isAuthLoading`**:
    -   **Type**: `boolean`
    -   **Description**: Indicates if an authentication-related action (like sign-in, sign-out, or token refresh) is currently in progress. This is useful for providing feedback to the user during these operations (e.g., disabling buttons or showing a spinner).

### 3. Actions/Functions

The context exposes the following functions to interact with the authentication state:

-   **`signIn(userId: string, password: string): Promise<void>`**:
    -   **Parameters**:
        -   `userId` (string): The user's ID (likely email).
        -   `password` (string): The user's password.
    -   **Description**:
        1.  Sets `isAuthLoading` to `true`.
        2.  Makes a POST request to `/api/auth/login` with `userId` and `password`.
        3.  If successful, it receives an `access_token` and `user` object.
        4.  Stores the `access_token` in Keychain using `Keychain.setGenericPassword()`.
        5.  Updates the `userToken` and `user` state.
        6.  Sets `isAuthLoading` to `false` in a `finally` block.
        7.  Throws an error if the login request fails.

-   **`signOut(): Promise<void>`**:
    -   **Description**:
        1.  Sets `isAuthLoading` to `true`.
        2.  Makes an authenticated POST request to `/api/auth/logout` using `fetchWithAuth`.
        3.  Resets the Keychain credentials using `Keychain.resetGenericPassword()`.
        4.  Sets `userToken` and `user` state to `null`.
        5.  Sets `isAuthLoading` to `false` in a `finally` block.

-   **`fetchWithAuth(url: string, options?: RequestInit): Promise<Response>`**:
    -   **Parameters**:
        -   `url` (string): The URL for the API request.
        -   `options` (RequestInit, optional): Standard `fetch` options.
    -   **Description**:
        1.  Retrieves the current `userToken`.
        2.  Adds the `Authorization: Bearer <userToken>` and `Content-Type: application/json` headers to the request.
        3.  Makes the `fetch` request.
        4.  If the response status is `401` (Unauthorized):
            a.  It attempts to refresh the token by POSTing the `expiredToken` to `/auth/api/protected/refresh`.
            b.  If token refresh is successful, it receives a new `access_token`.
            c.  The new `access_token` is stored in Keychain and `userToken` state is updated.
            d.  The original request is retried with the new token.
            e.  If token refresh fails, Keychain is reset, `userToken` and `user` are set to `null`, and a `401` response is returned.
        5.  Returns the `Response` object from the fetch call.

-   **`refreshUser(): Promise<void>`**:
    -   **Description**:
        1.  If `userToken` is not available, it does nothing.
        2.  Makes an authenticated GET request to `/api/users/me` using `fetchWithAuth`.
        3.  If successful, it updates the `user` state with the received user data.

### 4. AuthProvider

-   **Component**: `AuthProvider`
-   **Purpose**: `AuthProvider` is a React component that wraps the part of the application that needs access to the authentication context (typically the entire application).
-   **Initialization**:
    -   It uses `useState` to manage `userToken`, `user`, `isBootstrapping`, and `isAuthLoading`.
    -   It has a `useEffect` hook that runs on component mount (app startup):
        -   This effect (`bootstrapAsync`) attempts to load credentials (`authToken`) from `Keychain.getGenericPassword()`.
        -   If credentials exist, it sets the `userToken` state.
        -   It then calls `fetchWithAuth` to get the current user's data from `/api/users/me` and updates the `user` state.
        -   `isBootstrapping` is set to `false` after this process completes.
-   **Providing Context**: It renders `AuthContext.Provider` and passes a `contextValue` object (memoized with `useMemo`) containing the current state values and action functions.

### 5. Usage

Components that need access to authentication state or functions can consume the context using the `useContext` hook:

```typescript
import React, { useContext } from 'react';
import { AuthContext } from './AuthContext'; // Adjust path as needed

function MyComponent() {
  const { user, userToken, signIn, isAuthLoading } = useContext(AuthContext);

  if (isAuthLoading) {
    // Show loading indicator
  }

  if (user) {
    return <Text>Welcome, {user.nickname}!</Text>;
  } else {
    // Show login button or form
  }
  // ...
}
```

## Local Component State

While `AuthContext` manages global authentication state, individual components also manage their own internal state using the `useState` hook.

-   **Purpose**: Local state is used for data that is relevant only to a specific component and its children, or for UI-specific state that doesn't need to be shared globally.
-   **Typical Use Cases Observed**:
    -   **Form Inputs**: Storing the values of text inputs, selections, etc., before submission (e.g., email/password in `SignInScreen`, diary title/content in `WriteScreen`).
    -   **UI Toggles**: Managing the visibility or state of UI elements like modals (`modalVisible` in `SignupScreen`), date pickers (`showPicker` in `WriteScreen`), or calendar visibility (`calendarVisible` in `MainScreen`).
    -   **Component-Specific Data**: Storing data fetched or processed by a single component that isn't needed elsewhere (e.g., `filteredPosts` in `MainScreen`, `analysis` data in `AnalyzeScreen` before it's displayed).
    -   **Loading/Error States**: Managing loading and error states for component-specific API calls or operations.

This separation allows for a clear distinction between global concerns (like authentication) and local component responsibilities.
