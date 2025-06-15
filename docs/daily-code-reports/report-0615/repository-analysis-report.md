## Data Access Layer (JPA/QueryDSL) Analysis Report

This report analyzes the repository interfaces from `Auth-Server` and `CBT-back-diary` to assess data access patterns, querying techniques, and data coupling, particularly concerning `user_id`.

### 1. `Auth-Server` Repositories Analysis

*   **`UserRepository`:**
    *   Extends `JpaRepository<User, Long>` and `UserRepositoryCustom`.
    *   Provides derived queries: `Optional<User> findByUserName(String userName)`, `boolean existsByEmail(String email)`, `boolean existsByUserName(String userName)`.
    *   Delegates `updatePassword` and `findAllEmail` to `UserRepositoryCustom`.

*   **`UserRepositoryCustom` / `UserRepositoryImpl`:**
    *   `UserRepositoryCustom` interface defines `long updatePassword(String userId, String newPassword)` and `List<String> findAllEmail()`.
    *   `UserRepositoryImpl` implements these methods using **QueryDSL** (`JPAQueryFactory`, `QUser`).
        *   `updatePassword`: Type-safe update query to change a user's password based on `userId`.
        *   `findAllEmail`: Type-safe query to fetch all user emails.

*   **`AuthProviderRepository`:**
    *   Extends `JpaRepository<AuthProvider, Integer>`.
    *   Derived query: `Optional<AuthProvider> findByProviderName(String providerName)`.

*   **`UserAuthenticationRepository`:**
    *   Extends `JpaRepository<UserAuthentication, UserAuthenticationId>` (using an embedded ID).
    *   Derived query: `Optional<UserAuthentication> findByAuthProvider_ProviderNameAndSocialId(String providerName, String socialId)`.

*   **Summary of Querying Techniques (Auth-Server):**
    *   **Spring Data JPA Derived Queries:** Used for most standard find and existence check operations.
    *   **QueryDSL:** Employed for custom logic and update operations within `UserRepositoryImpl`, offering type-safe query construction.
    *   No direct use of JPQL or Native Queries was observed in the analyzed files.

### 2. `CBT-back-diary` Repositories Analysis

*   **`UserRepository`:**
    *   Extends `JpaRepository<User, Long>`.
    *   Derived queries: `Optional<User> findByEmail(String email)`, `Optional<User> findByUserName(String userName)`.

*   **`AuthProviderRepository`:**
    *   Extends `JpaRepository<AuthProvider, Integer>`.
    *   Derived query: `Optional<AuthProvider> findByProviderName(String providerName)`. (Functionally identical to `Auth-Server`'s).

*   **`UserAuthenticationRepository`:**
    *   Extends `JpaRepository<UserAuthentication, UserAuthenticationId>` (using `@IdClass`).
    *   Derived queries: `List<UserAuthentication> findByUserId(Long userId)`, `Optional<UserAuthentication> findFirstByUserId(Long userId)`.
    *   **JPQL Query (`@Query`):**
        *   `Optional<UserAuthentication> findByUserIdAndProviderName(@Param("userId") Long userId, @Param("providerName") String providerName)`: Uses a JPQL string to perform a specific lookup.

*   **`DiaryRepository`:**
    *   Extends `JpaRepository<Diary, Long>`.
    *   **Key Derived Queries using `userId`:**
        *   `Optional<Diary> findByIdAndUserId(Long diaryId, Long userId)`: Retrieves a diary by its ID and the associated user's ID.
        *   `Page<Diary> findAllByUserId(Long userId, Pageable pageable)`: Retrieves all diaries for a specific user with pagination.
    *   These methods highlight a strong coupling of diary data with user identity.

*   **Summary of Querying Techniques (CBT-back-diary):**
    *   **Spring Data JPA Derived Queries:** The primary method for data access, used for various find operations, including those involving `userId` in `DiaryRepository`.
    *   **JPQL (`@Query`):** Used selectively in `UserAuthenticationRepository` for queries where derived names might be complex or less clear.
    *   No use of QueryDSL or Native Queries was observed.

### 3. Comparison of Querying Techniques

*   **Common Ground:** Both projects heavily utilize **Spring Data JPA derived queries** for common lookup scenarios. This is efficient for straightforward queries and requires minimal boilerplate.
*   **Divergence in Custom Queries:**
    *   `Auth-Server` adopts **QueryDSL** for its custom repository implementation (`UserRepositoryImpl`). This provides type-safe query building in Java, reducing runtime errors and improving refactorability for complex queries (e.g., the `updatePassword` batch operation).
    *   `CBT-back-diary` uses **JPQL (`@Query`)** for custom queries that go beyond simple derived method names (e.g., in `UserAuthenticationRepository`). JPQL is powerful but string-based, lacking QueryDSL's compile-time type safety.
*   **Complexity Management:** `Auth-Server`'s choice of QueryDSL suggests a preference for handling more complex data manipulation or queries with Java-based type safety, potentially at the cost of an additional dependency and build step for Q-type generation. `CBT-back-diary` sticks to more standard JPA features.

### 4. Data Coupling in `CBT-back-diary`

*   **Strong `user_id` Coupling:** The `CBT-back-diary.DiaryRepository` exhibits a **very strong reliance on `user_id`** for data retrieval. Methods like `findByIdAndUserId` and `findAllByUserId` make `user_id` a mandatory parameter for accessing diary data.
*   **Implications:**
    *   This design inherently assumes that diaries are always accessed within the context of a specific user. It's a common and correct pattern for multi-tenant or user-centric applications.
    *   It underscores a **critical dependency on a reliable user context/identity mechanism.** The `userId` passed to these repository methods must be trustworthy.
    *   The current reliance on `MOCK_USER_ID_LONG` in the service layer, which is then used in these repository queries, is a significant vulnerability. For the application to be secure and functional, it *must* integrate with an authentication system (like `Auth-Server`) that can provide the actual, authenticated `userId`.
    *   The repository layer is well-prepared for user-specific data operations; the gap lies in obtaining the legitimate `userId` from a security context.

### 5. Absence of `DiaryRepository` in `Auth-Server`

*   Within the provided file structure for `Auth-Server`'s `com.authentication.auth.repository` package, there is **no `DiaryRepository` interface or implementation.**
*   **Potential Implications:**
    *   **Focus of `Auth-Server`:** This suggests that direct, sophisticated diary management (beyond what cascading from a `User` entity might offer) is not a primary, first-class concern of the `Auth-Server`'s core backend. Its main role seems to be authentication, user account details, provider management, and possibly related reporting or settings.
    *   **Alternative Handling:** If `Auth-Server` does interact with diary data (as implied by `User.diaries` list and unique entities like `DiaryReportLink`), this interaction might be:
        *   Through cascading persistence via the `User` entity.
        *   Handled in a different service or module not included in the current analysis scope.
        *   A feature that is less developed or exposed through its own repository layer in this part of the application.
    *   The presence of `Report` and `DiaryReportLink` entities in `Auth-Server` (from subtask 1.1.2) indicates some level of interaction with diaries, but the precise data access mechanism for `Diary` entities themselves (if not via `User`) is not evident from the provided repository files.

This concludes the analysis of the data access layer patterns for the specified repositories.
