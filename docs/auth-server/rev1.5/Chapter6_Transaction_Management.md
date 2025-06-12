# Chapter 6: Transaction Management and Data Consistency

## 6.1. Main Transaction Definition

### 6.1.1. Transaction Scope

Key operations requiring atomicity within the Emotion-based AI Diary Application include:

*   **User Registration:**
    *   Saving the new user's details to the `Users` table in MariaDB.
    *   If social login is part of the initial registration, saving associated records to `User_Authentication` and potentially `Auth_Provider` tables.
    *   The operation of sending a `UserCreatedEvent` to Kafka via `UserEventProducer.sendUserCreatedEvent()` is called within the user registration transaction in `UserService.join()`. The reliability of this event dispatch in conjunction with the database transaction needs careful consideration (see section 6.2).
*   **Diary Creation:**
    *   Saving the new `Diary` entry to the `Diary` table in MariaDB.
    *   Reliably dispatching a message to Kafka for AI analysis. Similar to user registration, the Kafka message send operation for analysis requests (if implemented in a transactional service method) would need its transactional properties defined (e.g., part of the main DB transaction or best-effort followed by monitoring).
*   **Settings Update:**
    *   When a user updates their settings, changes to the `User_custom_setting` table should be atomic, especially if multiple settings are updated at once.
*   **OAuth2 User Processing:**
    *   When handling an OAuth2 callback, operations like finding an existing user, creating a new user if one doesn't exist, and linking the social account via `User_Authentication` should be atomic to prevent partial user profiles.

### 6.1.2. Transaction Start and End Points

*   Transactions are primarily managed at the service layer using Spring's declarative transaction management.
*   **Start Point:** Typically, a transaction starts when a public method annotated with `@Transactional` in a Spring-managed service bean (e.g., `UserService`, assumed `DiaryService`, `SettingsService`, `Oauth2Service`) is invoked from a component outside the service (e.g., a Controller).
*   **End Point:**
    *   **Commit:** The transaction ends and commits if the annotated method completes successfully without throwing any runtime exceptions (or specific checked exceptions if configured for rollback).
    *   **Rollback:** The transaction rolls back if the annotated method throws a runtime exception (default Spring behavior) or any exception specified in `rollbackFor` attribute of `@Transactional`.

### 6.1.3. Transaction Isolation Level and Propagation Behavior (Spring Context)

*   **Isolation Level:**
    *   Spring's default isolation level when using `@Transactional` is typically `ISOLATION_DEFAULT`. This means it uses the default isolation level of the underlying database (MariaDB in this case). For MariaDB (InnoDB engine), the default isolation level is `REPEATABLE READ`.
    *   No specific deviations from this default (e.g., `READ_COMMITTED`, `SERIALIZABLE`) have been noted in the analyzed code. For most operations in this application, `REPEATABLE READ` should provide a good balance between consistency and concurrency. If specific operations were identified to cause contention or require stricter isolation, they could be configured with a different isolation level on their `@Transactional` annotation.
*   **Propagation Behavior:**
    *   The default propagation behavior in Spring is `PROPAGATION_REQUIRED`. This means that if a transaction already exists when a `@Transactional` method is called, the method will run within that existing transaction. If no transaction exists, a new one will be started.
    *   This is evident in `UserService.java` methods like `join()`, `getEmailByUserId()`, etc., which are annotated with `@Transactional` without specifying a particular propagation behavior, thus using the default.
    *   If helper methods within the same service are called by a public transactional method, they will join the existing transaction.

## 6.2. Transaction Flow Diagrams (Text-based Descriptions)

### User Registration (`UserService.join()`)

1.  `UsersController.join(joinRequest)` is called by a client request.
2.  `UsersController` calls `UserService.join(joinRequest)`.
3.  **`UserService.join()` method starts.** Spring AOP intercepts the call and begins a new transaction (due to `@Transactional` and default `PROPAGATION_REQUIRED`).
4.  **Input Validation:**
    *   `repository.existsByUserName(request.userId())` is called. This is a read operation within the transaction.
    *   If user ID already exists, an error response is built, and the method likely returns, leading to a transaction rollback (though no explicit exception is thrown here to trigger rollback automatically, the flow exits). For a clean rollback, an exception should ideally be thrown.
5.  **Entity Creation:** A `User` entity (`newUser`) is created and populated with data from `joinRequest`.
6.  **Password Encoding:** The user's password is encoded using `passwordEncoder.encode()`.
7.  **Database Save:** `UserRepository.save(newUser)` is called. This attempts to insert a new record into the `Users` table in MariaDB. This is a write operation within the transaction.
8.  **Kafka Event Publishing:** `userEventProducer.sendUserCreatedEvent(newUser.getId())` is called.
    *   **Consideration:** This Kafka send operation occurs *within* the same database transaction boundary defined by `@Transactional` on the `join()` method.
        *   If the Kafka send fails (e.g., Kafka broker unavailable) and throws an exception, it *could* cause the entire database transaction to roll back (user not saved). This creates a tight coupling.
        *   If the Kafka send is asynchronous and doesn't throw an exception back to the main thread that causes a rollback, the database transaction might commit even if the Kafka message fails to send.
        *   For better decoupling and reliability, an "outbox pattern" or ensuring the Kafka producer has its own transactional semantics (if supported and configured) or handling Kafka send failures explicitly (e.g., logging, retrying separately) after the main DB transaction commits would be preferable. Currently, it's a synchronous call within the DB transaction.
9.  **Commit/Rollback:**
    *   **Commit:** If `UserRepository.save()` and `userEventProducer.sendUserCreatedEvent()` (assuming it doesn't throw a rollback-inducing exception) complete successfully, and no other runtime exceptions occur, the transaction is committed upon exiting `UserService.join()`. The user record is permanently saved in MariaDB.
    *   **Rollback:** If `UserRepository.save()` fails (e.g., due to a database constraint violation not caught by earlier checks, or DB error), or if any other runtime exception is thrown within `UserService.join()`, the transaction is rolled back. The user record will not be saved in MariaDB.

### Diary Creation (Assumed `DiaryService.create()`)

1.  `DiaryController.create(diaryCreateRequest)` (assumed) is called.
2.  `DiaryController` calls `DiaryService.create(diaryCreateRequest, userId)`.
3.  **`DiaryService.create()` method starts.** Assuming it's annotated with `@Transactional`. A new transaction begins.
4.  **Entity Creation:** A `Diary` entity is created with content, title, and associated `User`.
5.  **Database Save:** `DiaryRepository.save(diaryEntity)` (assumed) is called to save the diary to MariaDB. This is a write operation within the transaction.
6.  **Send Message to Kafka:** `KafkaProducer.send("diary_analysis_requests", {diaryId, content})` (assumed) is called to request AI analysis.
    *   **Reliability:** Similar to user registration, if this Kafka send is part of the database transaction, its failure could roll back the diary save. Best practice often involves:
        *   Committing the database transaction for the diary first.
        *   Then, reliably sending the Kafka message (e.g., using an outbox pattern, or a transactional Kafka producer if the transaction manager can coordinate DB and Kafka, or by accepting that the message might fail and require monitoring/retry mechanisms).
        *   For this flow, we'll assume the primary transaction covers the MariaDB save. The Kafka send's atomicity with the DB save depends on specific Spring Kafka configuration.
7.  **Commit/Rollback:**
    *   **Commit:** If `DiaryRepository.save()` is successful, the core part of the transaction (saving the diary entry) commits. The success of the Kafka message dispatch depends on the strategy chosen above.
    *   **Rollback:** If `DiaryRepository.save()` fails, the transaction rolls back, and no diary is saved.

## 6.3. Data Consistency Maintenance Plan

### 6.3.1. RDB Constraints

Data integrity within MariaDB is maintained by:

*   **Primary Keys (PK):** Ensure uniqueness for each record in a table (e.g., `Users.id`, `Diary.id`). Defined in `schema.sql` with `AUTO_INCREMENT PRIMARY KEY`.
*   **Foreign Keys (FK):** Enforce referential integrity between tables. For example:
    *   `Diary.user_id` references `Users.id`.
    *   `User_Authentication.user_id` references `Users.id`.
    *   `User_Authentication.auth_provider_id` references `Auth_Provider.id`.
    *   `User_custom_setting.user_id` references `Users.id`.
    *   `User_custom_setting.setting_id` references `Settings_option.id`.
    *   `ON DELETE CASCADE` is used for relationships like `Users` to `Diary`, meaning if a user is deleted, their diaries are also deleted.
    *   `ON DELETE RESTRICT` is used for `Auth_Provider` in `User_Authentication`, preventing provider deletion if users are linked to it.
*   **Not Null Constraints:** Ensure essential fields always have a value (e.g., `Users.email`, `Users.password`, `Diary.content`). Defined in `schema.sql` and implied by non-primitive types in entities without `@Nullable`.
*   **Unique Constraints:** Prevent duplicate values in specified columns or combinations of columns (e.g., `Users.email`, `Users.user_name`, `Auth_Provider.provider_name`, composite unique key on `User_Authentication(auth_provider_id, social_id)`). Defined in `schema.sql`.

### 6.3.2. Application-Level Validation

*   **DTO Validation:** Request Data Transfer Objects (DTOs) use Jakarta Bean Validation annotations (e.g., `@NotBlank`, `@Size`, `@Email`, `@Pattern` as seen in `JoinRequest.java`, `DiaryCreateRequest.java`). These are typically enforced by Spring MVC using `@Valid` in controller method parameters. If validation fails, a `MethodArgumentNotValidException` is thrown, usually resulting in a 400 Bad Request response before service logic is invoked.
*   **Service-Level Validation:** Business logic within service methods performs additional checks (e.g., `UserService.join()` checks for `repository.existsByUserName()`).

### 6.3.3. Eventual Consistency for Asynchronous Processing (Kafka & AI Worker)

The AI analysis of diary entries is an asynchronous process decoupled by Kafka. This leads to eventual consistency for analysis results.

*   **Addressing AI Analysis Failures (User Feedback):**
    *   **Current State:** The AI worker consumes from Kafka and stores results in MongoDB. If the AI worker fails to process a message, the analysis result for that diary entry might be missing or delayed.
    *   **Proposed Strategy for Enhanced Consistency & Resilience:**
        1.  **Status Tracking in `Diary` Entity (MariaDB):**
            *   Add an `analysisStatus` field to the `Diary` entity/table in MariaDB.
            *   Possible enum values: `PENDING_ANALYSIS` (default on creation), `ANALYSIS_IN_PROGRESS`, `ANALYSIS_COMPLETE`, `ANALYSIS_FAILED`.
            *   When a diary is created, `DiaryService` sets this status to `PENDING_ANALYSIS`.
        2.  **AI Worker Updates Status:**
            *   **Option A (Callback API):** After processing (success or persistent failure), the AI Worker calls a secure internal API endpoint on the Backend (e.g., `PUT /api/internal/diaries/{diaryId}/analysis-status`) to update the `analysisStatus` in MariaDB and potentially store a brief error message if failed.
            *   **Option B (Kafka Response Topic):** The AI Worker sends a message to a different Kafka topic (e.g., "diary-analysis-results-feedback") indicating success or failure, including `diaryId` and status. A Kafka consumer within the Backend application would then update the `Diary` entity in MariaDB.
        3.  **Retry Mechanisms (AI Worker):** The AI Worker should implement retry logic for transient errors during analysis (e.g., temporary network issues when fetching models, brief unavailability of MongoDB). Exponential backoff is a common retry strategy.
        4.  **Dead-Letter Queue (DLQ) (Kafka):** For messages that consistently fail processing in the AI Worker (e.g., due to malformed data, unrecoverable model errors), configure Kafka to route these messages to a DLQ after a certain number of retry attempts by the consumer. This allows for:
            *   Manual inspection of failed messages.
            *   Debugging of issues in the AI worker or data.
            *   Potential for reprocessing messages from the DLQ after issues are resolved.
        5.  **Client UI:** The client application can reflect the `analysisStatus`. If `PENDING_ANALYSIS` or `ANALYSIS_IN_PROGRESS`, it can show an appropriate indicator. If `ANALYSIS_FAILED`, it could offer an option to retry the analysis (which would re-queue the request).

### 6.3.4. Distributed Transaction Considerations

*   **Current State:** The system primarily involves transactions within the Spring Boot backend interacting with MariaDB and Redis. The AI Worker's interaction with MongoDB is a separate process. True distributed transactions (e.g., two-phase commit across MariaDB and MongoDB) are not implemented and generally avoided due to complexity.
*   **Kafka Event Dispatch:** As noted in 6.2, sending a Kafka message from within a database transaction needs careful handling for atomicity.
    *   **Outbox Pattern:** A robust way to ensure reliable event dispatch is the outbox pattern. Changes to be published as events are first written to an "outbox" table within the same local database transaction as the business data. A separate process then reads from this outbox table and publishes messages to Kafka. Once successfully published, the event is marked as processed in the outbox table. This ensures the event is eventually sent even if Kafka is temporarily unavailable during the initial transaction, and the DB transaction doesn't roll back due to Kafka issues. This is not currently implemented but is a strong pattern for reliable eventing.
*   **Future Considerations:** If future microservices are introduced that consume Kafka messages and then need to update MariaDB (the same instance or another RDBMS), then patterns for maintaining data consistency across these distributed services would become critical. This might involve:
    *   **Saga Pattern:** For long-lived business transactions that span multiple services. Each service commits its local transaction and publishes an event. Subsequent services consume these events and execute their part of the saga. Compensating transactions are needed to roll back preceding steps if a later step fails.
    *   **Eventual consistency** would be the norm, with monitoring and reconciliation processes if needed.

## 6.4. Concurrency Control

### 6.4.1. Optimistic/Pessimistic Locking

*   **Optimistic Locking:**
    *   The `User.java` and `Diary.java` entity descriptions in `Data_Models_Entities.md` do not show a `@Version` annotation. Therefore, JPA's optimistic locking mechanism (which uses a version column to detect concurrent modifications) is **not currently implemented** for these entities.
    *   **Recommendation:** If high concurrency on updating specific entities (e.g., a shared setting, or potentially user profile updates) becomes an issue leading to lost updates, adding `@Version` to those entities would be a recommended approach. Spring Data JPA would then automatically check this version during updates and throw an `OptimisticLockingFailureException` if a conflict is detected, which can be handled by retrying the transaction or informing the user.
*   **Pessimistic Locking:**
    *   There is no evidence from the current code analysis (e.g., use of `LockModeType.PESSIMISTIC_WRITE` in repository queries) to suggest that pessimistic locking is being used. This is appropriate for most web application workloads as pessimistic locks can severely limit concurrency.

### 6.4.2. Redis for Distributed Locks

*   The `RedisService.java` code has been analyzed. It primarily focuses on storing and managing tokens (refresh, access) and email verification codes.
*   There are **no methods or Redis command patterns** (e.g., using `SETNX` with a TTL, or Redlock algorithm implementations) that indicate the use of Redis for distributed locks to manage concurrent access to shared resources across different service instances or operations.
*   **Recommendation:** If specific distributed operations are identified in the future that require exclusive access (e.g., a complex process that should only be run one instance at a time across a distributed deployment), Redis-based distributed locks could be a viable strategy. Libraries like Redisson simplify the implementation of distributed locks with Redis.
