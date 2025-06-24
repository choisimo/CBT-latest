## Service Layer Analysis: Auth-Server vs. CBT-back-diary

This report analyzes `UserService` from both projects and `DiaryService` from `CBT-back-diary` to identify duplicated logic, functional gaps, and dependencies.

### 1. `Auth-Server:UserService` Summary

*   **Primary Responsibilities:**
    *   **User Registration (`join`):**
        *   Validates against existing usernames and emails.
        *   Encodes passwords using `BCryptPasswordEncoder`.
        *   Sets default user role to "USER", `isPremium` to `false`, and `isActive` to "WAITING" (indicating a potential email verification step).
        *   Saves new `User` entities.
    *   **Email Retrieval (`getEmailByUserId`):** Fetches a user's email by their username.
    *   **Password Update (`UpdateUserPassword`):** Updates a user's password after encoding the new one.
    *   **Username Duplication Check (`checkUserNameIsDuplicate`):** Checks if a username is already taken.
*   **Dependencies:**
    *   `UserRepository`: For all database interactions with `User` entities.
    *   `BCryptPasswordEncoder`: For password hashing.
*   **Overall:** A functional, albeit basic, service for core user management tasks, including registration and password handling with security considerations (encoding, duplicate checks).

### 2. `CBT-back-diary:UserService` Summary

*   **Primary Responsibilities:**
    *   **Get Current User Details (`getCurrentUserDetails`):**
        *   Its sole function is to return a `UserDto` for the "current" user.
        *   **This is heavily mocked.** It uses a static `MOCK_USER_ID_LONG` (hardcoded to `1L`) to attempt to fetch a user from the database.
        *   If the mock user is found, it tries to determine a `providerType` (e.g., "NORMAL") by looking up a `UserAuthentication` record.
        *   `emailVerified` status is a placeholder (hardcoded to `true`).
        *   If the mock user isn't found in the DB, it returns a completely hardcoded `UserDto`.
*   **Use of `MOCK_USER_ID_LONG`:**
    *   This constant signifies the **absence of a real authentication and security context.** All operations are funneled through this mock ID, meaning the service cannot differentiate users or manage sessions.
*   **Comments Indicating Planned Integration:**
    *   Multiple comments explicitly state that the current mock approach is temporary and that user identification should eventually come from `Spring Security Context's Principal` after "full auth integration."
    *   Placeholders for fields like `emailVerified` also suggest planned enhancements.
*   **Dependencies:**
    *   `UserRepository`: To fetch the mock `User` by ID.
    *   `UserAuthenticationRepository`: To fetch `UserAuthentication` details for the mock user.
*   **Overall:** This service is currently a stub for fetching user details. It does not perform any actual user management operations like registration or password updates. It's designed to provide test data pending integration with a proper authentication system.

### 3. `CBT-back-diary:DiaryService` Summary

*   **Primary Responsibilities:**
    *   **Create Diary Post (`createDiaryPost`):** Creates and saves a new `Diary` entity.
        *   Associates the diary with a user fetched via `MOCK_USER_ID_LONG`.
    *   **Get Diary Post (`getDiaryPostById`):** Retrieves a single diary by its ID.
    *   **Update Diary Post (`updateDiaryPost`):** Updates a diary's title and content.
    *   **Map to DTO (`mapToDto`):** Converts `Diary` entities to `DiaryPostDto`.
*   **Use of `MOCK_USER_ID_LONG`:**
    *   All diary entries are associated with the user identified by `MOCK_USER_ID_LONG`, meaning no actual user-specific diary ownership is currently implemented.
*   **Comments Indicating Planned User Ownership Checks:**
    *   Multiple `TODO` comments and commented-out code blocks (e.g., `// TODO: Add user check: .findByIdAndUserId(diaryId, currentUserIdFromSecurityContext)`, `// if (!diary.getUser().getId().equals(MOCK_USER_ID_LONG))`) clearly show intent to implement user ownership checks for reading and updating diaries. This implies a future requirement to get the actual user ID from a security context.
*   **Dependencies:**
    *   `DiaryRepository`: For `Diary` entity database operations.
    *   `UserRepository`: To fetch the mock `User` entity.
*   **Overall:** This service provides basic CRUD functionalities for diaries but critically lacks real user ownership and security. It's heavily reliant on a mock user ID, with clear indications that proper security checks are planned.

### 4. Comparison of `UserService` Functionalities

*   **`Auth-Server:UserService`** is a functional user management service handling registration, password updates, and basic queries with security (password encoding, duplicate checks).
*   **`CBT-back-diary:UserService`** currently only *simulates* retrieving user details for a single mock user. It does not perform any write operations (registration, updates) or real checks.
*   **Overlap:**
    *   The conceptual need to **retrieve user details** is common. `CBT-back-diary:UserService`'s `getCurrentUserDetails` is a placeholder for what a real, authenticated user retrieval mechanism (expected from `Auth-Server`) would provide.
*   **Duplication/Strong Relation:**
    *   There is no direct code duplication because `CBT-back-diary:UserService`'s functionality is mocked.
    *   However, `CBT-back-diary:UserService` is *conceptually dependent* on the *type* of functionalities provided by `Auth-Server:UserService`. To become functional, it would need to integrate with a service that can actually identify and authenticate users.

### 5. Functional Gaps and Dependencies in `CBT-back-diary`

*   **Key Functional Gaps (requiring `Auth-Server`-like capabilities):**
    *   **User Registration:** Completely missing.
    *   **Authentication/Login:** No mechanism for users to log in or be identified.
    *   **Secure User Context:** No way to get the ID of the currently authenticated user (relies on mocks).
    *   **Password Management:** No features for setting, encoding, or updating passwords.
    *   **Token Management:** (If applicable) No token generation or validation.
    *   **Email Verification:** Logic for `emailVerified` field in `UserDto` is missing. `Auth-Server` has an `EmailVerification` entity, suggesting it handles this.
    *   **Role-Based Access Control:** Basic role assignment is in `Auth-Server`, but enforcement is not detailed in services yet.
*   **`CBT-back-diary:DiaryService` Dependency:**
    *   **Critically dependent.** The `DiaryService` explicitly needs to associate diaries with users. The `TODO` comments about adding user checks for diary access (ensuring users only access their own diaries) directly highlight the need for a robust authentication system that can provide the current user's identity. `Auth-Server` is designed to provide this. Without it, `CBT-back-diary` cannot securely manage user-specific data.

### 6. Diary Logic in `Auth-Server:UserService`

*   A review of `Auth-Server:UserService.java` confirms that it **does not contain any methods directly related to managing `Diary` entities** (CRUD operations, listing diaries, etc.).
*   Its scope is limited to user account management (join, password, email retrieval, duplicate checks).
*   While the `Auth-Server:User` entity *does* have a `@OneToMany` relationship to `Diary` entities, the `UserService` itself does not leverage or manage this relationship. Diary management in `Auth-Server` would likely reside in a dedicated `DiaryService` (if it exists).

## Conclusion on Duplication and Dependencies

There is no direct code duplication in the `UserService` implementations because `CBT-back-diary`'s version is largely a mock. However, `CBT-back-diary`'s services (`UserService` and `DiaryService`) exhibit a strong functional dependency on the capabilities typically provided by an authentication and user management server like `Auth-Server`. The `TODO` comments and use of mock IDs in `CBT-back-diary` are clear indicators that it is designed to integrate with such a system to become fully functional and secure.
