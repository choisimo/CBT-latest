# Data Access Layer Comparison Report

This report analyzes and compares the repository layer of `Auth-Server` and `CBT-back-diary` projects, focusing on their structure, custom queries, QueryDSL usage, and potential coupling.

## Repository Files Read:

**Auth-Server:**
*   `AuthProviderRepository.java`
*   `UserAuthenticationRepository.java`
*   `UserRepository.java`
*   `UserRepositoryCustom.java`
*   `UserRepositoryImpl.java`

*Note: Repositories for `Diary`, `Report`, `EmailVerification`, `SettingsOption`, and `UserCustomSetting` were not found in the `Auth-server/backend/src/main/java/com/authentication/auth/repository/` directory.*

**CBT-back-diary:**
*   `AuthProviderRepository.java`
*   `DiaryRepository.java`
*   `UserAuthenticationRepository.java`
*   `UserRepository.java`

## Analysis of Repositories:

### 1. Auth-Server Repositories

*   **`AuthProviderRepository`**:
    *   Extends `JpaRepository<AuthProvider, Integer>`.
    *   Methods:
        *   `Optional<AuthProvider> findByProviderName(String providerName)`: Standard derived query.

*   **`UserAuthenticationRepository`**:
    *   Extends `JpaRepository<UserAuthentication, UserAuthenticationId>`.
    *   Methods:
        *   `Optional<UserAuthentication> findByAuthProvider_ProviderNameAndSocialId(String providerName, String socialId)`: Derived query for finding a user authentication record by provider name and social ID.

*   **`UserRepository`**:
    *   Extends `JpaRepository<User, Long>` and `UserRepositoryCustom`.
    *   Methods:
        *   `Optional<User> findByUserName(String userName)`: Derived query.
        *   `boolean existsByEmail(String email)`: Derived query.
        *   `boolean existsByUserName(String userName)`: Derived query.

*   **`UserRepositoryCustom`**:
    *   Interface defining custom methods to be implemented, often using QueryDSL or complex JPQL.
    *   Methods:
        *   `long updatePassword(String userId, String newPassword)`
        *   `List<String> findAllEmail()`

*   **`UserRepositoryImpl`**:
    *   Implements `UserRepositoryCustom`.
    *   **QueryDSL Usage**: Yes, uses `JPAQueryFactory`.
        *   `updatePassword(String userId, String newPassword)`: Uses QueryDSL `update` query to change a user's password based on `userName`. This method is annotated with `@Transactional`.
        *   `findAllEmail()`: Uses QueryDSL `select` query to fetch all emails from the `User` table.
    *   **Paging**: The custom `findAllEmail` does not implement pagination. Standard JpaRepository methods would support it if exposed with `Pageable`.

### 2. CBT-back-diary Repositories

*   **`AuthProviderRepository`**:
    *   Extends `JpaRepository<AuthProvider, Integer>`.
    *   Methods:
        *   `Optional<AuthProvider> findByProviderName(String providerName)`: Standard derived query. (Identical method signature to Auth-Server's).

*   **`DiaryRepository`**:
    *   Extends `JpaRepository<Diary, Long>`.
    *   Methods:
        *   `Optional<Diary> findByIdAndUserId(Long diaryId, Long userId)`: Derived query to find a specific diary belonging to a specific user.
        *   `Page<Diary> findAllByUserId(Long userId, Pageable pageable)`: Derived query to find all diaries for a user, with pagination support.
    *   **Paging**: Explicitly uses `Pageable` for pagination in `findAllByUserId`.

*   **`UserAuthenticationRepository`**:
    *   Extends `JpaRepository<UserAuthentication, UserAuthenticationId>`.
    *   Methods:
        *   `List<UserAuthentication> findByUserId(Long userId)`: Derived query.
        *   `@Query("SELECT ua FROM UserAuthentication ua WHERE ua.user.id = :userId AND ua.authProvider.providerName = :providerName") Optional<UserAuthentication> findByUserIdAndProviderName(@Param("userId") Long userId, @Param("providerName") String providerName)`: Custom JPQL query.
        *   `Optional<UserAuthentication> findFirstByUserId(Long userId)`: Derived query to get a single authentication record for a user.
    *   **QueryDSL Usage**: No. Uses derived queries and JPQL.

*   **`UserRepository`**:
    *   Extends `JpaRepository<User, Long>`.
    *   Methods:
        *   `Optional<User> findByEmail(String email)`: Derived query.
        *   `Optional<User> findByUserName(String userName)`: Derived query (for finding by what is likely a nickname or unique username).
    *   **QueryDSL Usage**: No. Uses derived queries.

## Summary of Findings:

*   **Repository Structure:**
    *   Both projects use Spring Data JPA, extending `JpaRepository` for basic CRUD and derived query functionalities.
    *   Auth-Server employs a custom repository interface (`UserRepositoryCustom`) and implementation (`UserRepositoryImpl`) pattern to incorporate QueryDSL for more complex or specific update/query operations.
    *   CBT-back-diary primarily relies on derived query methods and occasional `@Query` annotations with JPQL.

*   **QueryDSL Usage:**
    *   **Auth-Server:** Actively uses QueryDSL in `UserRepositoryImpl` for operations like batch password updates and fetching specific projections (all emails). The implementation is standard, using `JPAQueryFactory` and Q-types.
    *   **CBT-back-diary:** No QueryDSL usage was observed in the provided repository files. It favors Spring Data JPA's derived queries and JPQL for custom needs.

*   **Coupling and Cross-Domain Data Access:**
    *   **CBT-back-diary's `DiaryRepository`** queries diaries based on `userId`. This is a natural and common form of coupling where the `Diary` entity has a foreign key to the `User` entity. While it uses `userId`, it doesn't appear to fetch extensive user profile details from the `User` table within these repository methods themselves; it primarily uses `userId` for filtering. The `DiaryService` would then be responsible for potentially enriching this data if needed.
    *   **CBT-back-diary's `UserAuthenticationRepository`** also queries by `userId`, which is inherent to its purpose of linking users to authentication providers.
    *   **Auth-Server's** provided repositories (`User`, `AuthProvider`, `UserAuthentication`) are focused on its core domain of authentication and user management. Since `Auth-Server` also has its own `Diary` entity (as identified in previous tasks), any interaction its own `DiaryRepository` (if it exists and is similar to CBT-back-diary's) would have with its `User` entity would be internal to its domain. Without seeing Auth-Server's repositories for `Diary`, `Report`, etc., it's difficult to assess further coupling from that side.
    *   The `AuthProviderRepository` is virtually identical in both projects, suggesting that if `AuthProvider` data were centralized, this repository could be shared or one version could be made canonical.

*   **Complexity of Data Access Patterns:**
    *   **Auth-Server:** The use of QueryDSL for specific operations in `UserRepositoryImpl` indicates a willingness to use more powerful query tools when needed, potentially for performance or complex criteria. The separation into `UserRepositoryCustom` and `UserRepositoryImpl` is a good practice for organizing custom logic.
    *   **CBT-back-diary:** The data access patterns are primarily straightforward, relying on the expressive power of Spring Data JPA's derived queries and JPQL. This approach is often simpler for common use cases. The use of `Pageable` in `DiaryRepository` shows an understanding of handling potentially large datasets.

*   **Paging Mechanisms:**
    *   `CBT-back-diary` explicitly uses `Pageable` in `DiaryRepository` for paginated results, which is a standard Spring Data JPA feature.
    *   `Auth-Server`'s custom QueryDSL method `findAllEmail` does not use pagination, but standard JpaRepository methods it inherits would support it.

**Overall:**
Both projects leverage Spring Data JPA effectively. Auth-Server introduces QueryDSL for more complex custom queries in its `UserRepository`, while CBT-back-diary sticks to derived queries and JPQL for its repository layer as seen in the provided files. The coupling observed is largely expected due to relational dependencies (e.g., diaries belonging to users). The missing repositories in Auth-Server for entities like `Diary`, `Report`, etc., mean a full comparison of data access patterns for those entities cannot be made at this time. If these entities are indeed managed by Auth-Server, their repositories would be needed for a complete picture.
