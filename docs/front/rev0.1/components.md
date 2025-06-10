# Screen Component Documentation

This document provides details about the React components used as screens in the application.

---

## 1. `EmailVerificationScreen.tsx`

-   **Component Name**: `EmailVerificationScreen`
-   **Purpose**: Allows users to verify their email address by submitting a code sent to their email.
-   **Props**:
    -   `navigation`: `NativeStackScreenProps<AuthStackParamList, 'VerifyEmail'>`. Used to navigate to other screens within the `AuthStack`.
-   **Local State**:
    -   `code` (string, initial: `''`): Stores the verification code entered by the user. Updated by `setCode`.
    -   `loading` (boolean, initial: `false`): Indicates if the verification API call is in progress. Updated by `setLoading`.
-   **Navigation**:
    -   Does not directly navigate but relies on `AuthContext`'s `refreshUser` to trigger a change in authentication state, which `RootNavigator` handles.
-   **Context Usage**:
    -   `AuthContext`:
        -   `refreshUser()`: Called after successful code verification to update the user's authentication status (including `emailVerified`).
        -   `fetchWithAuth()`: Used to make the API call to the `/api/auth/verify-email` endpoint.

---

## 2. `SignInScreen.tsx`

-   **Component Name**: `SignInScreen`
-   **Purpose**: Allows users to sign in to the application using their email and password.
-   **Props**:
    -   `navigation`: `NativeStackScreenProps<AuthStackParamList, 'SignIn'>`. Used to navigate to the `SignUpScreen`.
-   **Local State**:
    -   `email` (string, initial: `''`): Stores the email entered by the user. Updated by `setEmail`.
    -   `password` (string, initial: `''`): Stores the password entered by the user. Updated by `setPassword`.
-   **Navigation**:
    -   `navigation.navigate('SignUp')`: Navigates to the `SignUpScreen` when the "회원가입" (Sign Up) button is pressed.
-   **Context Usage**:
    -   `AuthContext`:
        -   `signIn(email, password)`: Called to perform the login operation. This function handles API calls and secure storage of tokens.
        -   `isAuthLoading` (boolean): Used to show an `ActivityIndicator` on the login button while authentication is in progress.

---

## 3. `SignupScreen.tsx`

-   **Component Name**: `SignupScreen`
-   **Purpose**: Allows new users to register for an account by providing their email, password, name, and nickname, and agreeing to terms.
-   **Props**:
    -   `navigation`: `NativeStackScreenProps<AuthStackParamList, 'SignUp'>`. Used to navigate back to `SignInScreen` after successful registration.
-   **Local State**:
    -   `email` (string, initial: `''`): Stores the user's email. Updated by `setEmail`.
    -   `password` (string, initial: `''`): Stores the user's password. Updated by `setPassword`.
    -   `userName` (string, initial: `''`): Stores the user's real name. Updated by `setUserName`.
    -   `nickname` (string, initial: `''`): Stores the user's nickname. Updated by `setNickname`.
    -   `modalVisible` (boolean, initial: `true`): Controls the visibility of the terms and conditions modal. Updated by `setModalVisible`.
    -   `agreePersonal` (boolean, initial: `false`): Tracks if the user agreed to the personal information policy. Updated by `setAgreePersonal`.
    -   `agreeTerms` (boolean, initial: `false`): Tracks if the user agreed to the service terms. Updated by `setAgreeTerms`.
-   **Navigation**:
    -   `navigation.replace('SignIn')`: Navigates to the `SignInScreen` after a successful signup, replacing the current screen in the stack.
-   **Context Usage**:
    -   None directly. It makes a public API call to `/api/public/join`.

---

## 4. `AnalyzeScreen.tsx`

-   **Component Name**: `AnalyzeScreen`
-   **Purpose**: Displays the AI-generated analysis of a user's diary entry. It can also show a progress state if the analysis is not yet complete.
-   **Props**:
    -   `route`: `NativeStackScreenProps<AppStackParamList, 'Analyze'>`. Contains `postId` as a parameter, which is the ID of the diary post to analyze/display.
-   **Local State**:
    -   `isLoading` (boolean, initial: `true`): Indicates if the analysis data is being fetched. Updated by `setIsLoading`.
    -   `error` (string, initial: `''`): Stores any error message encountered during data fetching. Updated by `setError`.
    -   `inProgress` (object | null, initial: `null`): Stores information about an ongoing analysis (message, progress, estimatedRemaining). Updated by `setInProgress`.
        -   Type: `{ message: string; progress: number; estimatedRemaining: string } | null`
    -   `analysis` (`AnalysisResult` | null, initial: `null`): Stores the fetched analysis data. Updated by `setAnalysis`.
        -   `AnalysisResult` type includes `id`, `emotionDetection`, `emotionSummary`, `automaticThought`, `promptForChange`, `alternativeThought`, `status`, `confidence`, `analyzedAt`.
-   **Navigation**:
    -   None directly.
-   **Context Usage**:
    -   `AuthContext`:
        -   `fetchWithAuth()`: Used to make authenticated GET requests to `/api/diaries/{postId}/analysis`.
        -   `user`: Used to check if a user is logged in before attempting to fetch data.

---

## 5. `MainScreen.tsx`

-   **Component Name**: `MainScreen`
-   **Purpose**: The main screen of the application. It displays a list of diary entries, allows users to search for entries, view entries by date using a calendar, and navigate to write a new entry.
-   **Props**:
    -   `navigation`: `NativeStackScreenProps<AppStackParamList, 'Main'>`. Used for navigating to `ViewScreen` (to view a post) and `WriteScreen` (to create a new post).
-   **Local State**:
    -   `searchText` (string, initial: `''`): Stores the current search query. Updated by `setSearchText`.
    -   `selectedDate` (string | null, initial: `null`): Stores the date selected from the calendar (YYYY-MM-DD format). Updated by `setSelectedDate`.
    -   `calendarVisible` (boolean, initial: `false`): Toggles the visibility of the calendar UI. Updated by `setCalendarVisible`.
    -   `allDates` (string[], initial: `[]`): Stores an array of dates (YYYY-MM-DD) that have diary entries, used for marking dates on the calendar. Updated by `setAllDates`.
    -   `currentPage` (number, initial: `0`): Tracks the current page for pagination of diary entries. Updated by `setCurrentPage`.
    -   `totalCount` (number, initial: `0`): Total number of diary entries available based on the current filter (search/date). Updated by `setTotalCount`.
    -   `filteredPosts` (`Post[]`, initial: `[]`): Array of diary posts to be displayed in the list. Updated by `setFilteredPosts`.
        -   `Post` type: `{ id: string; title: string; date: string }`
-   **Navigation**:
    -   `navigation.navigate('View', { postId: item.id })`: Navigates to `ViewScreen` when a post item is pressed.
    -   `navigation.navigate('Write')`: Navigates to `WriteScreen` when the floating action button (FAB) for creating a new post is pressed.
-   **Context Usage**:
    -   `AuthContext`:
        -   `user`: Used to check if a user is logged in before fetching data.
        -   `fetchWithAuth()`: Used for all authenticated API calls to `/api/diaries` (fetching dates for calendar, fetching posts with pagination, search, and date filtering).
        -   `isAuthLoading`: (Not directly used in UI logic, but available from context).

---

## 6. `ViewScreen.tsx`

-   **Component Name**: `ViewScreen`
-   **Purpose**: Displays the details of a specific diary post, including its date, title, and content. It also provides options to edit the post or view/request its AI analysis.
-   **Props**:
    -   `route`: `NativeStackScreenProps<AppStackParamList, 'View'>`. Contains `postId` as a parameter, identifying the post to display.
    -   `navigation`: `NativeStackScreenProps<AppStackParamList, 'View'>`. Used for navigating to `WriteScreen` (to edit the post) or `AnalyzeScreen` (to view/request analysis).
-   **Local State**:
    -   `post` (`PostData` | null, initial: `null`): Stores the fetched diary post data. Updated by `setPost`.
        -   `PostData` type: `{ id: string; date: string; title: string; content: string; aiResponse: boolean }`
    -   `error` (string, initial: `''`): Stores error messages if fetching the post fails. Updated by `setError`.
-   **Navigation**:
    -   `navigation.navigate('Write', { postId })`: Navigates to `WriteScreen` for editing the current post.
    -   `navigation.navigate('Analyze', { postId: post.id })`: Navigates to `AnalyzeScreen` to view or initiate analysis for the current post.
-   **Context Usage**:
    -   `AuthContext`:
        -   `fetchWithAuth()`: Used to make authenticated GET request to `/api/diaryposts/{postId}` to fetch post details, and POST request to `/api/diaries/{postId}/analysis` to initiate AI analysis if not already done.
        -   `user`: Used to check for user login status.
        -   `isAuthLoading`: Used to show a loading indicator while the initial authentication check might be in progress.

---

## 7. `WriteScreen.tsx`

-   **Component Name**: `WriteScreen`
-   **Purpose**: Allows users to create a new diary entry or edit an existing one. It includes fields for date, title, and content.
-   **Props**:
    -   `route`: `NativeStackScreenProps<AppStackParamList, 'Write'>`. Can optionally contain `postId` as a parameter. If `postId` is present, the screen operates in "edit mode"; otherwise, it's in "create mode".
    -   `navigation`: `NativeStackScreenProps<AppStackParamList, 'Write'>`. Used for navigating back after saving or to other screens (e.g., `AnalyzeScreen` after creating a new post).
-   **Local State**:
    -   `postId` (string | undefined, from `route.params`): Stores the ID of the post being edited.
    -   `date` (`Date` | undefined, initial: `undefined`): Stores the selected date for the diary entry. Updated by `setDate` via `DateTimePicker`.
    -   `showPicker` (boolean, initial: `false`): Controls the visibility of the `DateTimePicker`. Updated by `setShowPicker`.
    -   `title` (string, initial: `''`): Stores the title of the diary entry. Updated by `setTitle`.
    -   `content` (string, initial: `''`): Stores the content of the diary entry. Updated by `setContent`.
    -   `isLoading` (boolean, initial: `false`): Indicates if an API call (fetching existing post, creating, or updating) is in progress. Updated by `setIsLoading`.
-   **Navigation**:
    -   `navigation.goBack()`: Called after successfully updating an existing post or if fetching an existing post fails.
    -   `navigation.navigate('Analyze', { postId: newPostId })`: Called after successfully creating a new post, navigating to the `AnalyzeScreen` with the new post's ID.
-   **Context Usage**:
    -   `AuthContext`:
        -   `fetchWithAuth()`: Used for all authenticated API calls:
            -   GET `/api/diaryposts/{postId}` (to fetch existing post data in edit mode).
            -   PUT `/api/diaryposts/{postId}` (to update an existing post).
            -   POST `/api/diaryposts` (to create a new post).
        -   `userToken`: Used to check if the user is logged in before making API calls.

---
