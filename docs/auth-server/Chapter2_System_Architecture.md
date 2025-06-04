# Chapter 2: System Architecture

## 2.1. Overall System Architecture (High-Level Architecture)

### 2.1.1. Conceptual Architecture Diagram
(Note: Visual diagrams `systemArchitecture0421.png` and `systemArchitectureDetail.jpg` were not found in the repository. The following description is based on the layers mentioned in the issue and inferred from the codebase.)

The system is envisioned as a multi-layered architecture:

1.  **Client Layer:** This layer represents the user interface, presumed to be a Web Application and/or Mobile Application. It interacts with the backend services via HTTP/HTTPS requests.
2.  **API Gateway (Nginx):** Nginx acts as a reverse proxy, handling incoming client requests. It can be responsible for SSL termination, load balancing across backend instances, and routing requests to the appropriate services.
3.  **Application Layer (Spring Boot Backend - "Auth-Server"):** This is the core backend service built with Java and Spring Boot. It handles business logic, user authentication, data management, and communication with other services. It appears to be a monolithic service for the features analyzed, though it's named "Auth-Server" which might imply a microservices context for the broader system. This layer includes:
    *   **Auth Service:** Integrated within the Spring Boot application using Spring Security. It manages user registration, login (local and OAuth2), JWT issuance and validation, and access control.
    *   **Application Services:** Modules for User Management, Diary Management (partially implemented), and Settings Management (documented but not implemented).
4.  **Message Queue (Apache Kafka):** Kafka is used for asynchronous communication. For instance, the Application Layer produces messages (e.g., when a new user is created or a diary entry needs analysis) which are consumed by other services, particularly the AI Processing Layer.
5.  **AI Processing Layer (Python AI Worker):** This is a separate component, described as a Python AI Worker. It consumes messages from Kafka (e.g., diary content), performs AI-based emotion analysis and thought suggestion, and likely stores its results in a database (e.g., MongoDB).
6.  **Database Layer:** This layer consists of multiple database technologies:
    *   **MariaDB:** Used for primary relational data storage, such as user accounts, roles, and potentially core diary entries.
    *   **MongoDB:** Likely used for storing AI analysis results from the Python worker, and potentially other non-relational data like logs or user preferences that benefit from a flexible schema.
    *   **Redis:** Used for caching (e.g., user sessions, frequently accessed data), storing refresh tokens, and managing temporary data like email verification codes.
7.  **Monitoring Layer:**
    *   **Prometheus:** Collects metrics from various system components.
    *   **Grafana:** Visualizes metrics collected by Prometheus, providing dashboards for monitoring system health and performance.
    *   **ELK Stack (Elasticsearch, Logstash, Kibana):** Provides centralized logging, allowing for searching, analyzing, and visualizing log data from all parts of the system.

### 2.1.2. Role and Responsibilities of Each Layer

*   **Client Layer:** Provides the user interface for interacting with the application (e.g., writing diary entries, viewing analysis, managing settings).
*   **API Gateway (Nginx):**
    *   Acts as the single entry point for all client requests.
    *   Provides SSL termination to secure client-server communication.
    *   Can perform load balancing if multiple instances of the backend services are deployed.
    *   Routes requests to the correct backend service (primarily the Application Layer).
*   **Application Layer (Spring Boot Backend):**
    *   **Auth Service:** Manages user identity, authentication (credentials, OAuth2), and authorization (access control to APIs). Issues and validates JWTs.
    *   **Application Services:** Implements the core business logic for user management, diary entries (CRUD), and (eventually) settings. Interacts with the Database Layer for persistence and the Message Queue for asynchronous tasks.
*   **Message Queue (Kafka):**
    *   Decouples the Application Layer from the AI Processing Layer.
    *   Enables asynchronous processing of tasks like AI analysis, improving responsiveness of the Application Layer.
    *   Provides fault tolerance and scalability for message-driven communication.
*   **AI Processing Layer (Python AI Worker):**
    *   Consumes data (e.g., diary entries) from Kafka.
    *   Performs computationally intensive AI/ML tasks (emotion analysis, thought detection/suggestion).
    *   Stores the results of its analysis, likely in MongoDB.
*   **Database Layer:**
    *   **MariaDB:** Ensures ACID properties for critical relational data (users, authentication details).
    *   **MongoDB:** Provides a flexible schema for storing complex, evolving data like AI analysis results or logs.
    *   **Redis:** Improves performance by caching frequently accessed data and manages session-related tokens and temporary codes.
*   **Monitoring Layer:**
    *   Provides visibility into the system's health, performance, and operational status.
    *   Facilitates troubleshooting and debugging through centralized logging and metrics.

### 2.1.3. Component Interfaces and Data Exchange Protocols

*   **Client Layer to API Gateway (Nginx):** HTTP/HTTPS.
*   **API Gateway (Nginx) to Application Layer (Spring Boot):** HTTP (typically within a trusted network).
*   **Application Layer to Database Layer:**
    *   **MariaDB:** JDBC (Java Database Connectivity) via Spring Data JPA.
    *   **MongoDB:** MongoDB Java Driver via Spring Data MongoDB.
    *   **Redis:** Redis Java client (e.g., Lettuce or Jedis) via Spring Data Redis.
*   **Application Layer to Message Queue (Kafka):** Kafka client library (Java) for producing messages.
*   **Message Queue (Kafka) to AI Processing Layer:** Kafka client library (Python) for consuming messages.
*   **AI Processing Layer to Database Layer (MongoDB):** MongoDB Python Driver (e.g., PyMongo).
*   **Internal communications within Spring Boot application:** Java method calls.
*   **Monitoring Data Collection:** Various protocols depending on the exporter (e.g., HTTP for Prometheus scraping).

## 2.2. Detailed Module Architecture

### 2.2.1. Authentication/Authorization Service (Auth Service)
This service is embedded within the Spring Boot backend application.

*   **Spring Security Filter Chain Configuration and Operation:**
    *   **`SecurityConfig.java` (`com.authentication.auth.configuration.security.SecurityConfig`):**
        *   Disables CSRF, form login, and HTTP Basic authentication, opting for a stateless session management policy (suitable for JWTs).
        *   Configures CORS.
        *   Defines URL-based authorization rules using `authorizeHttpRequests`. Public paths (`/api/public/**`, static resources) are permitted for all. Other paths can be restricted based on roles (though `userRestrict`, `adminRestrict`, etc., are currently empty arrays, implying most authenticated routes might be broadly accessible after login, or specific restrictions are handled programmatically or via method security if used).
        *   Sets up `PrincipalDetailService` as the `UserDetailsService` for loading user-specific data.
        *   Provides a `BCryptPasswordEncoder` bean for password hashing.
        *   An `AuthenticationManager` bean is exposed.
        *   OAuth2 login configuration is commented out (`http.oauth2Login`), suggesting OAuth2 is handled via custom controllers (`Oauth2Controller`) rather than Spring Security's default OAuth2 login flow.
    *   **`SecurityFilterConfig.java` (`com.authentication.auth.config.SecurityFilterConfig`):**
        *   This class seems to be an alternative or older way of registering filters using `FilterRegistrationBean`. It registers `AuthenticationFilter` and `AuthorizationFilter`. The main `SecurityConfig` does not explicitly add these filters to the Spring Security filter chain via `http.addFilterBefore/After`, which is the more common way. However, if these filters are Spring `@Component`s that extend base Spring Security filters or are processed by Spring, they might still be active. The `AuthenticationFilter` extends `AbstractAuthenticationProcessingFilter` and is configured to handle `/api/auth/login`.
        *   The `AuthorizationFilter` (custom, extends `AbstractSecurityFilter`) is also registered here.
    *   **`AuthenticationFilter.java` (`com.authentication.auth.filter.AuthenticationFilter`):**
        *   Extends `AbstractAuthenticationProcessingFilter` and is specifically mapped to the `/api/auth/login` POST endpoint.
        *   `attemptAuthentication()`: Reads `LoginRequest` (userId, password) from the request, creates a `UsernamePasswordAuthenticationToken`, and authenticates it using the `AuthenticationManager`.
        *   `successfulAuthentication()`: If authentication succeeds, it generates JWT (access and refresh tokens) using `TokenProvider` (which internally uses `JwtUtility`). The refresh token is stored in Redis via `RedisService`. Both tokens are also set as HttpOnly cookies. The access token is returned in the response body.
        *   `unsuccessfulAuthentication()`: Returns an error response if authentication fails.
    *   **`AuthorizationFilter.java` (`com.authentication.auth.filter.AuthorizationFilter`):**
        *   A custom filter that extends `AbstractSecurityFilter`. Its role is to perform additional authorization checks.
        *   It checks if the request path starts with `/api/admin/` and if the authenticated user has an "ADMIN" role. If not, it returns a FORBIDDEN error.
        *   It uses `FilterRegistry` and `PathPatternFilterCondition` to potentially skip filtering for public paths.
        *   It appears intended to run *before* `JwtVerificationFilter` (if `JwtVerificationFilter` is responsible for initial JWT validation and setting SecurityContext). However, `JwtVerificationFilter` itself was not directly requested for reading, but `AuthorizationFilter` refers to it. *The exact interplay and ordering of custom filters vs. Spring Security's standard filters like `JwtAuthenticationFilter` or `BearerTokenAuthenticationFilter` would need careful review of the complete filter chain setup, which can be complex.*
    *   **`JwtVerificationFilter.java` (not read, but referenced):** This filter is likely responsible for validating the JWT from the `Authorization` header for requests to protected endpoints (other than `/api/auth/login`). It would parse the token, validate it, and set the `Authentication` object in the `SecurityContextHolder`.

*   **JWT Issuance, Validation, and Re-issuance Logic:**
    *   **Issuance (`JwtUtility.java`, `TokenProvider.java` used by `AuthenticationFilter`):**
        *   `JwtUtility.buildToken()`: Creates access and refresh tokens. It uses `HS512` algorithm. Claims include `userId` and `role`. Access token validity and refresh token validity are configurable via `AppProperties`.
    *   **Validation (`JwtUtility.java`, likely used by `JwtVerificationFilter`):**
        *   `JwtUtility.validateJWT()`: Parses the JWT, checks signature and expiration.
        *   `JwtUtility.getAuthentication()`: Parses the JWT, extracts `userId` and roles, and creates a `UsernamePasswordAuthenticationToken` with `PrincipalDetails` to be set in the SecurityContext.
    *   **Re-issuance (`TokenController.java`, `TokenService.java`):**
        *   Endpoint `/auth/api/protected/refresh` (in `TokenController`, though API doc shows `/api/auth/refresh`).
        *   `TokenService.refreshToken()`:
            *   Validates the incoming expired access token and the refresh token (provider + token from cookie/request).
            *   Checks if the refresh token exists in Redis and matches the provided one.
            *   If valid, `JwtUtility.refreshToken()` is called to issue a new access token using claims from the expired token. The old refresh token might be invalidated and a new one issued, or the existing one reused depending on policy (current code seems to reuse it if still valid, but also saves the new access token to Redis keyed by the refresh token).

*   **OAuth2 Social Login Processing Flow (`Oauth2Controller.java`, `Oauth2Service.java`):**
    *   The client application first calls `/api/public/oauth2/login_url/{provider}` (defined in `AuthController.java` from API docs, not `Oauth2Controller.java`) to get the provider's authorization URL.
    *   User authenticates with the OAuth2 provider (e.g., Google, Kakao, Naver).
    *   Provider redirects back to a callback URL like `/oauth2/callback/{provider}` (handled by `Oauth2Controller.java`).
    *   `Oauth2Controller` receives the authorization code (`tempCode`) from the request body (differs from API docs which say query params for the generic callback).
    *   It calls `Oauth2Service.handleOauth2Login()`.
    *   `Oauth2Service`:
        1.  Exchanges the `tempCode` for OAuth2 access/refresh tokens with the respective provider (e.g., `getKakaoTokens()`, `getNaverTokens()`, `getGoogleTokens()`).
        2.  Fetches the user's profile from the provider using the OAuth2 access token (e.g., `getKakaoUserProfile()`).
        3.  The provider's refresh token (if obtained) is stored in Redis via `RedisService`, keyed by the OAuth user ID and provider.
        4.  Calls `saveOrUpdateOauth2User()`:
            *   Checks if a `UserAuthentication` record exists for the given provider and social ID.
            *   If exists, updates user details if necessary (e.g., email, active status).
            *   If not, creates a new `User` and `UserAuthentication` record. The `userName` for the new `User` is derived from email or OAuth ID, with attempts to ensure uniqueness.
        5.  Generates application-specific JWT (access and refresh tokens) for the user using `JwtUtility`.
        6.  Sets the application's refresh token in an HttpOnly cookie and returns the application's access token and user profile in the response.

### 2.2.2. Application Service (Application Layer - Spring Boot Monolith)

*   **Structure and Responsibilities of Main Domain Modules:**
    *   **User Management:**
        *   **`UsersController.java` (`com.authentication.auth.controller.UsersController`):**
            *   `/api/users/public/join`: Handles new user registration. Calls `UserService.join()`.
            *   `/api/users/public/check/userName/IsDuplicate`, `/api/users/public/check/userId/IsDuplicate`: Checks for duplicate usernames/user IDs. Calls `UserService.checkUserNameIsDuplicate()`.
            *   `/api/users/public/clean/userTokenCookie`: Clears the refresh token cookie for logout.
        *   **`UserService.java` (`com.authentication.auth.service.users.UserService`):**
            *   `join()`: Validates new user data, encodes password using `BCryptPasswordEncoder`, saves the new `User` entity. Produces a `UserCreatedEvent` to Kafka via `UserEventProducer`.
            *   `getEmailByUserId()`: Retrieves user email.
            *   `UpdateUserPassword()`: Updates user password.
            *   `checkUserNameIsDuplicate()`: Checks for username duplication.
        *   **`User.java` (`com.authentication.auth.domain.User`):** JPA entity representing user data (ID, password, email, `userName` as login ID, roles, timestamps, active status, premium status, relations to Diary and UserAuthentication).
        *   **`PrincipalDetailService.java` (`com.authentication.auth.service.security.PrincipalDetailService`):** Implements `UserDetailsService` to load `UserDetails` (as `PrincipalDetails`) for Spring Security using `UserRepository`.
    *   **Diary Management:**
        *   **`DiaryController.java`:** Specified in `API_Documentation.md` (Section 6) but **not found** in the scanned codebase at `com.authentication.auth.controller`.
        *   **`DiaryService.java`:** Specified in `API_Documentation.md` but **not found** in the scanned codebase at `com.authentication.auth.service`.
        *   **`Diary.java` (`com.authentication.auth.domain.Diary`):** JPA entity exists. It includes fields for `id`, `user` (association), `createdAt`, `updatedAt`, `title`, `content`, `alternativeThought`, and `isNegative`. This indicates that the core data model for diaries is defined.
        *   **Responsibilities (Inferred from API Docs & Entity):** CRUD operations for diary entries, linking entries to users, and potentially triggering AI analysis (which would involve Kafka).
    *   **Settings Management:**
        *   **`SettingsController.java`:** Specified in `API_Documentation.md` (Section 7) but **not found** in the scanned codebase.
        *   **`SettingsService.java`:** Specified in `API_Documentation.md` but **not found** in the scanned codebase.
        *   **Entities (`SettingsOption.java`, `UserCustomSetting.java`):** Mentioned in the subtask prompt but **not found** in `com.authentication.auth.domain`.
        *   **Responsibilities (Inferred from API Docs):** Managing application-level settings and user-specific customizable settings.

*   **Asynchronous Processing with Kafka:**
    *   **Kafka Producer Logic:**
        *   **`UserEventProducer.java` (in `kafka-module`):**
            *   Used by `UserService.join()` when a new user is created.
            *   Sends a `UserCreatedEvent` (containing `userId`) to the "user-created-events" Kafka topic. This message is serialized as JSON.
        *   **Diary Analysis (Hypothetical):** If `DiaryService` were implemented, it would likely contain Kafka producer logic. When a diary entry is created or updated, `DiaryService` would send the diary content (or an ID to fetch it) to a Kafka topic (e.g., "diary-analysis-requests"). This message would then be consumed by the Python AI Worker.
    *   **Kafka Consumer Configuration:**
        *   No Kafka consumer configuration or listener code (e.g., `@KafkaListener`) was found within the `com.authentication.auth` backend codebase or the provided `kafka-module` structure.
        *   This implies that the primary consumer for events like `UserCreatedEvent` or hypothetical diary analysis requests is an external service (the Python AI Worker).
        *   The `kafka-module/build.gradle` includes `spring-kafka` and `spring-boot-starter-json`, providing the necessary dependencies for Kafka integration.

### 2.2.3. AI Analysis Module (AI Processing Layer - Python AI Worker)

*   **Description:** This module is explicitly described as a "Python AI Worker" and its codebase is presumed to be separate from the Java backend monorepo.
*   **Assumed Role:**
    *   Consumes diary entries from a specific Kafka topic (e.g., "diary-analysis-requests").
    *   Performs Natural Language Processing (NLP) tasks for:
        *   Emotion analysis (identifying primary emotions in the text).
        *   Automatic thought detection (identifying potentially negative or unhelpful thought patterns).
        *   Alternative thought suggestion (providing healthier or more balanced perspectives).
    *   Stores the results of its analysis, likely into the MongoDB database, associated with the diary entry (e.g., in a separate collection or embedded within a diary document if MongoDB schema allows).
    *   It might also consume other events, like `UserCreatedEvent`, if user-specific AI model initialization or background processing is needed.
*   **Potential Models/Libraries (To be confirmed from AI Worker's documentation/code):**
    *   **NLP Libraries:** NLTK, spaCy, Transformers (from Hugging Face).
    *   **Emotion Analysis:** Could use pre-trained models from Transformers (e.g., models fine-tuned for emotion classification) or custom models trained using libraries like scikit-learn, TensorFlow, or PyTorch.
    *   **Thought Detection/Suggestion:** This is a more complex task. It might involve rule-based systems, pattern matching, or more advanced ML models trained on cognitive behavioral therapy (CBT) principles or similar datasets. Techniques could range from keyword spotting to sequence-to-sequence models for generating alternative thoughts.
*   **Kafka Consumer Integration:**
    *   The Python AI Worker would use a Python Kafka client library (e.g., `kafka-python`) to connect to the Kafka cluster.
    *   It would subscribe to relevant topics (e.g., "diary-analysis-requests").
    *   Upon receiving a message, it would deserialize the diary content, perform the AI analysis, and then write the results to MongoDB, possibly including the original diary ID for linkage.

## 2.3. Infrastructure Architecture

### 2.3.1. Deployment Environment Configuration
*   **Docker:** Used for containerizing the Spring Boot application ("Auth-Server"), the Python AI Worker, and potentially other services like databases or Kafka itself for development or isolated deployments. This ensures consistency across different environments.
*   **Nginx:**
    *   Acts as a reverse proxy, routing external traffic to the Spring Boot application.
    *   Handles SSL termination, offloading HTTPS processing from the application server.
    *   Can be configured for load balancing if multiple instances of the backend are running.
    *   May serve static content directly.

### 2.3.2. Network Configuration Diagram
(Note: A visual diagram would typically be provided here. The description below is textual.)

*   **Public Network:** Clients (web/mobile) access the system via the internet through Nginx's public IP address/domain name over HTTPS (port 443).
*   **DMZ/Public Subnet:** Nginx would typically reside here.
*   **Internal/Private Network:**
    *   The Spring Boot application servers, Kafka cluster, AI Worker instances, and databases (MariaDB, MongoDB, Redis) would reside in a private network, not directly accessible from the internet.
    *   Nginx forwards requests to the Spring Boot application servers over HTTP/HTTPS on their respective ports.
    *   Application servers communicate with Kafka, AI Workers, and databases within this private network.
    *   Communication between components should be secured (e.g., Kafka with SSL/SASL, databases with SSL and strong credentials).
*   **Firewall Rules:** Strict firewall rules would be in place to control traffic between network zones and components.

### 2.3.3. Database Server Configuration

*   **MariaDB:**
    *   **Role:** Primary storage for relational data. This includes:
        *   User accounts (`User` entity: ID, credentials, email, profile info, roles).
        *   User authentication providers (`AuthProvider`, `UserAuthentication` entities for OAuth).
        *   Core diary entries (`Diary` entity: content, title, timestamps, user link), although some aspects like AI analysis might go to MongoDB.
        *   (If implemented) Application settings (`SettingsOption`) and user-specific settings (`UserCustomSetting`).
*   **MongoDB:**
    *   **Role:** Storage for data that benefits from a flexible, document-based schema. This includes:
        *   **AI Analysis Results:** Detailed outputs from the emotion analysis and thought suggestion process, linked to specific diary entries. This data can be complex and vary per entry.
        *   **Logs:** Potentially used by the ELK stack for storing application and system logs if Elasticsearch is part of ELK.
        *   Other non-relational or rapidly evolving data structures.
*   **Redis:**
    *   **Role:** In-memory data store for high-performance data access.
        *   **Caching:** Caching frequently accessed data to reduce database load (e.g., user profiles, configuration settings).
        *   **Session Management:** While the app is stateless (JWT), Redis could be used for related purposes like rate limiting or tracking active users if needed.
        *   **Token Storage:** Storing JWT refresh tokens (`RedisService.saveRToken()`) to manage their validity and allow for revocation or re-issuance. Also stores OAuth provider refresh tokens.
        *   **Email Verification Codes:** Storing temporary codes sent for email verification (`RedisService.saveEmailCode()`).

### 2.3.4. Message Queue System Configuration (Apache Kafka)
*   **Role:** Acts as a central nervous system for asynchronous communication, decoupling producers (like the Spring Boot Application Service) from consumers (like the Python AI Processing Layer).
*   **Topics (Examples):**
    *   `user-created-events`: Used by `UserService` to publish when a new user is created. Consumed by downstream services (potentially the AI worker for profile initialization or other modules).
    *   `diary-analysis-requests` (Hypothetical): Would be used by `DiaryService` to send diary entries for AI analysis.
    *   `ai-analysis-results` (Hypothetical): The AI worker might publish results to this topic if other services need to react to completed analyses in real-time, or it might write directly to MongoDB.
*   **Brokers:** A Kafka cluster would consist of one or more brokers for fault tolerance and scalability.
*   **Partitions:** Topics would be partitioned to allow for parallel consumption and increased throughput.
*   **Replication:** Data would be replicated across brokers to prevent data loss.

### 2.3.5. Monitoring and Logging System Configuration

*   **Prometheus:**
    *   **Role:** Collects time-series metrics from the Spring Boot application (via Actuator and micrometer-registry-prometheus), Nginx, Kafka, databases, and the Python AI worker.
    *   Metrics include request rates, error rates, response times, JVM metrics, system resource usage, Kafka topic lag, etc.
*   **Grafana:**
    *   **Role:** Visualizes the metrics stored in Prometheus through customizable dashboards.
    *   Provides alerts based on metric thresholds to notify administrators of issues.
*   **ELK Stack (Elasticsearch, Logstash, Kibana):**
    *   **Logstash:** Aggregates logs from all components (Spring Boot app, Nginx, Python AI worker, databases, Kafka). It parses and transforms these logs before sending them to Elasticsearch.
    *   **Elasticsearch:** Stores and indexes the processed logs, making them searchable.
    *   **Kibana:** Provides a web interface for searching, analyzing, and visualizing logs stored in Elasticsearch. Helps in troubleshooting and understanding application behavior.
```
