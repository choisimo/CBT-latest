# Service Layer Comparison Report

This report analyzes and compares key service classes from the `Auth-Server` and `CBT-back-diary` projects, focusing on their responsibilities, logic, and potential overlaps.

## 1. UserService Comparison

### Auth-Server: `com.authentication.auth.service.users.UserService`

*   **Primary Responsibilities:**
    *   User registration (joining the service).
    *   Retrieving user email by user ID.
    *   Updating user passwords.
    *   Checking for username (user ID) duplication.
*   **Key Methods:**
    *   `join(JoinRequest request)`: Creates a new user, encodes the password, checks for existing `userName` and `email`. Sets initial state (`WAITING`, `isPremium=false`).
    *   `getEmailByUserId(String userId)`: Fetches a user's email by their `userName`.
    *   `UpdateUserPassword(String userId, String temporalPassword)`: Updates a user's password after encoding the new one.
    *   `checkUserNameIsDuplicate(String userName)`: Checks if a `userName` already exists.
*   **Transactional Usage:** All public methods are annotated with `@Transactional`. No explicit `readOnly` attribute is used, implying default read-write transaction behavior.

### CBT-back-diary: `com.ossemotion.backend.service.UserService`

*   **Primary Responsibilities:**
    *   Currently, its main function is to retrieve details for the "current" user. This is heavily reliant on a `MOCK_USER_ID_LONG` for now, indicating it's under development or expects user identification via Spring Security context in a fully integrated system.
*   **Key Methods:**
    *   `getCurrentUserDetails()`: Fetches user details (ID, nickname, email, emailVerified, providerType, role) and returns them as a `UserDto`. It attempts to fetch from the database using the mock ID and falls back to hardcoded mock data if not found. It also tries to determine `providerType` from `UserAuthenticationRepository`.
*   **Transactional Usage:** `getCurrentUserDetails()` is annotated with `@Transactional(readOnly = true)`.

### Comparison:

*   **Similar Names/Purposes:**
    *   While there isn't a direct method-for-method match for all functionalities, both services deal with user data. Auth-Server's `getEmailByUserId` and CBT-back-diary's `getCurrentUserDetails` both involve fetching user data, but the latter is broader and context-dependent (current user).
*   **Business Logic Duplication:**
    *   Currently, there's minimal direct duplication of implemented business logic. Auth-Server's `UserService` handles core user lifecycle events like registration and password changes. CBT-back-diary's `UserService` is primarily focused on retrieving user information for display, with much of the user creation/management logic likely intended to be handled by OAuth flows (as suggested by its `UserAuthenticationRepository` usage) or yet-to-be-implemented features.
    *   If CBT-back-diary were to implement its own direct registration or full profile management, there would be a high potential for duplication with Auth-Server's `UserService`.
*   **Differences in Functionality:**
    *   **Auth-Server:** Provides direct user registration (`join`) with password hashing, email/username validation, and password update capabilities. It sets an initial user state (e.g., `isActive = "WAITING"`).
    *   **CBT-back-diary:** Focused on retrieving a `UserDto` for an assumed "current" user. It includes logic to determine the `providerType` (e.g., "NORMAL", "google") by querying `UserAuthentication` data. It uses a placeholder for `emailVerified`.
*   **Transactional Annotation Usage:**
    *   Auth-Server uses `@Transactional` without `readOnly` for all its user service methods. This is appropriate for methods like `join` and `UpdateUserPassword` that perform writes. For read-only methods like `getEmailByUserId` and `checkUserNameIsDuplicate`, explicitly setting `readOnly = true` could be a minor optimization.
    *   CBT-back-diary correctly uses `@Transactional(readOnly = true)` for `getCurrentUserDetails` as it's a data retrieval method.

## 2. CBT-back-diary: `com.ossemotion.backend.service.DiaryService`

*   **Primary Responsibilities:** Manages CRUD (Create, Read, Update) operations for diary entries.
*   **Core Functions:**
    *   `createDiaryPost(CreateDiaryPostRequest request)`: Creates a new diary entry. It associates the diary with a user (currently using `MOCK_USER_ID_LONG`). Sets title, content, and diary date.
    *   `getDiaryPostById(Long diaryId)`: Retrieves a specific diary post by its ID.
    *   `updateDiaryPost(Long diaryId, UpdateDiaryPostRequest request)`: Updates the title and content of an existing diary post. It notes that AI-related fields (`aiAlternativeThoughts`, `isNegative`) are typically not updated via this endpoint.
    *   `mapToDto(Diary diary)`: A private helper method to convert the `Diary` entity to a `DiaryPostDto` for responses.
*   **Transactional Usage:**
    *   `createDiaryPost` and `updateDiaryPost` are annotated with `@Transactional` (appropriate for write operations).
    *   `getDiaryPostById` is annotated with `@Transactional(readOnly = true)` (appropriate for read operations).
*   **Notes:** The service currently uses a `MOCK_USER_ID_LONG` for user association, indicating that full user context integration (likely from Spring Security) is pending. It includes TODOs for adding user checks to ensure users can only access/modify their own diaries.

## 3. Auth-Server: Specialized Services

### `com.authentication.auth.service.oauth2.Oauth2Service`

*   **Role:** Central service for handling OAuth2 authentication with external providers (Kakao, Naver, Google).
*   **Responsibilities:**
    *   **Token Exchange:** Manages the OAuth2 authorization code grant flow, exchanging temporary codes for access and refresh tokens from the respective providers.
    *   **User Profile Retrieval:** Uses the obtained access tokens to fetch user profile information from the OAuth providers.
    *   **User Provisioning/Linking:**
        *   Checks if a user associated with the OAuth ID already exists in the local database (via `UserAuthentication` and `UserRepository`).
        *   If the user exists, it may update their details based on the fresh profile information.
        *   If the user doesn't exist, it creates a new `User` record and a corresponding `UserAuthentication` record to link the local user account with the external OAuth identity. It includes logic for generating a unique `userName` (login ID), potentially using email or a combination of provider and OAuth ID.
    *   **Application Token Issuance:** After successful OAuth authentication and user provisioning/linking, it generates application-specific JWTs (access token) using `JwtUtility`.
    *   **Refresh Token Management:** Stores the OAuth provider's refresh token (if available) in Redis and sets it as an HTTP-only cookie in the response.
    *   **External API Calls:** Uses `RestTemplate` to communicate with OAuth provider APIs.
*   **Transactional Usage:** Methods like `saveOrUpdateOauth2User` and `handleOauth2Login` are `@Transactional` because they involve multiple database operations (reads, writes to `User`, `AuthProvider`, `UserAuthentication`) that need to be atomic.

### `com.authentication.auth.service.token.TokenService`

*   **Role:** Manages the refreshing of application-specific JWTs.
*   **Responsibilities:**
    *   Receives an expired JWT and the provider information.
    *   Validates the expired token (checks signature, claims, but allows expiration).
    *   If the expired token is structurally valid, it generates a new JWT.
    *   Crucially, it checks if the corresponding refresh token (obtained from the HTTP request cookie) exists and is valid in Redis for the given user and provider. This ensures that only legitimate refresh attempts are processed.
*   **Transactional Usage:** The `refreshToken` method is `@Transactional`. This might be for ensuring consistency if `redisService` or `jwtUtility` internally perform operations that benefit from a transaction, or as a general safety measure.

### `com.authentication.auth.service.security.PrincipalDetailService`

*   **Role:** Implements Spring Security's `UserDetailsService` interface.
*   **Responsibilities:**
    *   `loadUserByUsername(String username)`: This method is called by Spring Security during the authentication process. It takes a `username` (which maps to the `userName` field in the `User` entity) and retrieves the corresponding `User` from the `UserRepository`.
    *   It then wraps the `User` entity in a `PrincipalDetails` object (which implements `UserDetails`) that Spring Security can use to perform authentication checks (e.g., password comparison) and for authorization purposes (e.g., loading roles/authorities).
*   **Transactional Usage:** Not explicitly annotated with `@Transactional`. Spring Data JPA methods like `findByUserName` are often transactional by default or inherit the transactional context from the calling method if one exists. For `UserDetailsService`, operations are typically read-only and short-lived.

## Summary of Key Findings:

*   **UserService Duplication & Differences:**
    *   The `UserService` in `Auth-Server` is more comprehensive, handling user registration, password management, and basic lookups.
    *   The `UserService` in `CBT-back-diary` is currently minimal, focused on retrieving (mocked) user details, likely as a placeholder for integration with a more robust authentication system (potentially like the one in `Auth-Server`).
    *   There's significant potential for consolidating user management logic if these services were to be merged, with Auth-Server's `UserService` providing a more complete base.
    *   Transactional management is generally appropriate in both, with `readOnly = true` used correctly in `CBT-back-diary`.

*   **DiaryService (CBT-back-diary):**
    *   Provides standard CRUD operations for diary entries (`create`, `get`, `update`).
    *   Relies on a mock user ID for now, indicating the need for integration with an actual authentication context.
    *   Handles mapping between `Diary` entities and `DiaryPostDto`s.

*   **Auth-Server Specialized Services:**
    *   `Oauth2Service`: Provides a complete OAuth2 login solution for multiple providers, including user provisioning and application token generation. This is a significant piece of functionality not present in the `CBT-back-diary` files reviewed.
    *   `TokenService`: Handles JWT refresh logic, relying on Redis for refresh token validation.
    *   `PrincipalDetailService`: Integrates with Spring Security to load user details for authentication.

*   **Significant Differences in Implementation:**
    *   **User Creation:** Auth-Server has direct user registration and OAuth-based user creation. CBT-back-diary's user creation mechanism isn't fully evident from the provided `UserService` but seems to lean towards OAuth if its `UserAuthenticationRepository` usage is an indicator.
    *   **Authentication Handling:** Auth-Server has a fully fledged OAuth2 and JWT token-based authentication system. CBT-back-diary's services currently use mock user data, implying its authentication layer is either separate or not yet fully integrated into these specific services.
    *   **Scope:** Auth-Server services cover a broader range of authentication, authorization, and user management features. CBT-back-diary services (as shown) are more narrowly focused on their specific domain entities (User DTO retrieval, Diary CRUD).

If the goal is to merge or integrate these two systems, the `Auth-Server` provides a more complete authentication and core user management foundation, while `CBT-back-diary` provides the diary-specific business logic.
