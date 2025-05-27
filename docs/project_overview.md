### **Project Analysis Report (project-analyze.md)**

#### **1. Project Overview**

*   **Project Goal:**
    *   Development of a backend server with a primary focus on authentication and authorization.
    *   Likely for an 'Emotion Diary' service, managing user accounts, diary entries, and notifications (inferred from `Diary`, `Report` domains and the `oss-emotion` directory name).
*   **Key Architecture:**
    *   **Monolithic Architecture based on Spring Boot:** The `Auth-server` handles not only authentication but also various domains like Diary, User, Report, etc.
    *   **Potential for Microservices:** The existence of a `TODO/MicroServiceArchitect.md` file suggests future considerations for transitioning to a microservices architecture.
    *   **Use of Nginx:** Inferred from `oss-emotion/DOCUMENT/NGINX` documentation, Nginx is used as a web server/reverse proxy.

#### **2. Implemented Features**

*   **2.1. Authentication & Authorization:**
    *   **Native Login:** ID/Password based login (`AuthController`, `AuthenticationFilter`).
    *   **Social Login (OAuth2):** Integration with Google, Kakao, Naver (`Oauth2Controller`, `application-oauth-*.properties`).
    *   **JWT-based Token Management:** Issuance and validation of Access and Refresh Tokens (`JwtUtility`, `TokenController`, `TokenService`). Refresh Tokens are likely stored and managed in Redis (`redisConfig`).
*   **2.2. User Management:**
    *   Features: Sign-up, profile viewing, user statistics retrieval (`UsersController`, `UserService`).
    *   Management of user roles and statuses (`User$UserRole`, `User$UserStatus`).
*   **2.3. Email Service:**
    *   SMTP-based email sending (`smtpConfig`, `EmailController`).
    *   Used for sending verification codes, temporary passwords, and admin notices (e.g., `email_verification.html` templates).
*   **2.4. Server-Sent Events (SSE):**
    *   Real-time, unidirectional data transfer using SSE (`SseController`, `SseService`) (e.g., for new alerts, announcements).
*   **2.5. Domain-specific Features:**
    *   **Diary Management:** DTOs for diary creation, viewing, and analysis requests exist (`DiaryCreateRequest`, `DiaryDetailResponse`). (Controller not explicitly visible in current structure, but functionality inferred from DTOs and domain).
    *   **Report Management:** Generation and management of reports based on diary analysis (`Report` domain).
*   **2.6. Admin Features:**
    *   **Dynamic Filter Management:** `AdminFilterController` likely allows dynamic addition/deletion/viewing of security filter conditions, aiming for flexible API access control.

#### **3. Module Structure & Relations**

*   **`config`, `configuration`:** Core application settings for security, CORS, Redis, OAuth2, database, etc.
*   **`controller`:** Receives HTTP requests and delegates business logic processing to the `service` layer.
*   **`service`:** Executes core business logic, interacts with the database via `repository`, and utilizes utility classes like `JwtUtility`.
*   **`domain`:** Contains JPA Entities, representing the application's core data model.
*   **`dto`:** Data Transfer Objects for communication between layers, clearly defining API request/response specifications.
*   **`repository`:** Manages database CRUD operations using Spring Data JPA and QueryDSL.
*   **`filter`:** Integrated into the Spring Security filter chain for pre/post-processing related to security (authn/authz).
*   **`others/constants`:** Defines security-related constants (JWT headers, keys, etc.) to enhance code consistency and maintainability.

#### **4. Key Process Flowcharts**

*   **4.1. Native Login Flow:**
    1.  `Client` -> `POST /auth/login` (ID/PW)
    2.  `AuthenticationFilter` -> Intercepts request
    3.  `UserService` & `PrincipalDetailService` -> Validate user information
    4.  `TokenProvider`/`JwtUtility` -> Generate Access/Refresh Tokens
    5.  `RedisService` -> Store Refresh Token
    6.  `Client` <- `TokenDto` (Return tokens)
*   **4.2. Social Login Flow:**
    1.  `Client` -> `GET /oauth2/authorization/{provider}`
    2.  `Social Provider` -> Authenticates user, then redirects to `redirect_uri` with `code`
    3.  `Client` -> `GET /auth/login/oauth2/code/{provider}?code=...`
    4.  `Oauth2Controller` & `Oauth2Service` -> Request Access Token from social provider using code and fetch user info
    5.  `UserService` -> If user is new, register; otherwise, update info
    6.  `TokenProvider`/`JwtUtility` -> Issue service's own JWT
    7.  `Client` <- `OAuth2LoginResponse` (Return tokens and user info)
*   **4.3. JWT Authenticated API Request Flow:**
    1.  `Client` -> `GET /api/some-resource` (Header: `Authorization: Bearer ...`)
    2.  `JwtVerificationFilter` -> Extract and validate token from header
    3.  `SecurityContextHolder` -> If validation successful, register user authentication info
    4.  `Controller` -> Execute business logic
    5.  `Client` <- `ApiResponse` (Return request result)

#### **5. UX/UI Analysis**

*   **Login/Sign-up:** Offers both native registration and social login options for user convenience.
*   **Email Notifications:** Enhances user experience with email feedback for key events (welcome, temporary password) using templates from `templates/email`.
*   **Error Handling:** Provides clear and consistent error messages for frontend developers and users via `ErrorController`, `CustomException`, `ErrorResponse`.
*   **Real-time Updates:** SSE implementation allows for immediate user feedback (e.g., notifications).

#### **6. Technology Stack (Inferred and Documented)**
*   **Backend:** Spring Boot, Spring Security
*   **Authentication:** JWT, OAuth2
*   **Database:** MariaDB (assumed for main data), Redis (for refresh tokens, caching), MongoDB (mentioned in skill-stack)
*   **Data Access:** Spring Data JPA, QueryDSL
*   **Web Server/Proxy:** Nginx
*   **Real-time Communication:** Server-Sent Events (SSE)
*   **Email:** SMTP
*   **Build/Dependency Management:** Gradle
*   **Infrastructure/Deployment:** Docker (from skill-stack)
*   **AI/ML (mentioned in skill-stack, purpose in this project TBC):** Python, TensorFlow, PyTorch
*   **Monitoring (from skill-stack):** Prometheus, Grafana, ElasticSearch

