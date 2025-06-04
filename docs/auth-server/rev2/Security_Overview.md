# Security Overview

This document outlines the security architecture of the Auth-Server Spring Security application, focusing on authentication, authorization, and the use of JSON Web Tokens (JWT).

## Core Principles

*   **JWT-based Authentication**: Users authenticate once and receive a JWT, which is then used for subsequent requests.
*   **Filter-based Security**: Custom Spring Security filters handle authentication and authorization logic.
*   **Role-based Authorization**: Access to certain API endpoints is restricted based on user roles (e.g., `ROLE_ADMIN`).
*   **CORS Configuration**: Proper Cross-Origin Resource Sharing (CORS) settings are in place to allow secure communication from different origins.

## Key Components

### `SecurityFilterConfig`

*   **Purpose**: Configures the Spring Security filter chain and registers custom filters.
*   **Location**: `com.authentication.auth.config.SecurityFilterConfig.java`
*   **Functionality**:
    *   Registers `AuthenticationFilter` and `AuthorizationFilter` into the Spring Security filter chain.
    *   Ensures that these custom filters are executed in the correct order.

### `AuthenticationFilter`

*   **Purpose**: Handles user authentication, typically by processing login requests and generating JWTs.
*   **Location**: `com.authentication.auth.filter.AuthenticationFilter.java`
*   **Functionality**:
    *   Intercepts login requests (e.g., `/api/auth/login`).
    *   Validates user credentials.
    *   Upon successful authentication, generates an Access Token and a Refresh Token.
    *   Adds the Access Token to the `Authorization` header and the Refresh Token as an `HttpOnly` cookie.

### `AuthorizationFilter`

*   **Purpose**: Handles authorization by validating JWTs and enforcing access control based on user roles and requested paths.
*   **Location**: `com.authentication.auth.filter.AuthorizationFilter.java`
*   **Functionality**:
    *   Intercepts incoming requests to protected resources.
    *   Extracts and validates the JWT from the `Authorization` header.
    *   Parses user roles from the JWT.
    *   Checks if the authenticated user has the necessary roles to access the requested path.
    *   Specifically, it checks for `ROLE_ADMIN` or `ADMIN` for paths starting with `SecurityConstants.ADMIN_API_PATH`.
    *   Allows public paths (defined in `SecurityConstants.PUBLIC_PATHS`) to bypass authorization checks.
    *   Handles token refresh requests.

### `SecurityConstants`

*   **Purpose**: Defines constants related to security, including JWT secrets, token expiration times, and various API path patterns.
*   **Location**: `com.authentication.auth.others.constants.SecurityConstants.java`
*   **Key Constants**:
    *   `JWT_SECRET`
    *   `ACCESS_TOKEN_EXPIRATION_TIME`
    *   `REFRESH_TOKEN_EXPIRATION_TIME`
    *   `PUBLIC_API_PATH`: Base path for public endpoints (e.g., `/api/public/**`).
    *   `ADMIN_API_PATH`: Base path for admin-only endpoints (e.g., `/api/admin/**`).
    *   Specific public paths like `/api/auth/login`, `/api/auth/join`, `/api/public/emailSend`, `/swagger-ui.html`, etc.

### `WebConfig` (CORS Configuration)

*   **Purpose**: Configures Cross-Origin Resource Sharing (CORS) to allow web browsers to make requests to the API from different origins.
*   **Location**: `com.authentication.auth.config.WebConfig.java`
*   **Functionality**:
    *   Allows requests from specified origins (e.g., `http://localhost:8080`, `http://localhost:3000`, `http://localhost:7078`, `http://localhost:7077`, `https://oss-emotion.nodove.com`).
    *   Permits common HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`).
    *   Allows all headers.
    *   Exposes `Authorization` header to the client.
    *   Allows credentials (cookies, HTTP authentication).
    *   Sets a max age for pre-flight requests.

## Authentication Flow

1.  **User Login**: A user sends a `POST` request to `/api/auth/login` with their credentials.
2.  **AuthenticationFilter**: Intercepts the request, validates credentials, and if successful, generates a JWT Access Token and Refresh Token.
3.  **Token Issuance**: The Access Token is returned in the `Authorization` header, and the Refresh Token is set as an `HttpOnly` cookie.
4.  **Subsequent Requests**: For protected resources, the client includes the Access Token in the `Authorization: Bearer <token>` header.
5.  **AuthorizationFilter**: Intercepts the request, validates the Access Token, extracts user roles, and checks if the user is authorized to access the requested resource.

## Authorization Flow

1.  **Request Interception**: The `AuthorizationFilter` intercepts all incoming requests.
2.  **Public Path Check**: If the requested path is a public path (defined in `SecurityConstants`), the filter proceeds without further authorization checks.
3.  **Token Validation**: For protected paths, the filter attempts to validate the JWT. If invalid or missing, an unauthorized response is returned.
4.  **Role-Based Access**: If the token is valid, the filter checks if the user's roles (extracted from the JWT) grant access to the requested resource. The `AuthorizationFilter` is primarily responsible for this enforcement.
    *   **`ROLE_USER`:** Users with this role typically have access to functionalities related to their own data and general application features.
        *   **Examples of accessible endpoints:**
            *   `GET /api/diaries`, `POST /api/diaries/{diaryId}`: Managing their own diary entries.
            *   `GET /api/settings`, `PUT /api/settings`: Accessing and updating their personal settings.
            *   `POST /api/protected/sendEmailPassword`: Requesting a password reset for their own account (if already logged in and want to change).
            *   Access to SSE subscription: `GET /api/protected/sse/subscribe`.
        *   They cannot access administrative endpoints.
    *   **`ROLE_ADMIN`:** Users with this role have elevated privileges, allowing them to manage the system, users, and access all data.
        *   **Examples of accessible endpoints:**
            *   All endpoints under `/api/admin/**` (as defined by `SecurityConstants.ADMIN_API_PATH`). This includes user management functionalities if exposed via such paths.
            *   `AdminFilterController` endpoints (e.g., `GET /admin/filters`, `POST /admin/filters/{filterId}/conditions`): For managing dynamic system filters.
            *   Potentially, endpoints for viewing system logs, metrics, or performing other administrative tasks.
        *   Admins can typically also access all user-level functionalities.
    *   The distinction is enforced by the `AuthorizationFilter` checking the roles extracted from the JWT against the requirements of the requested path. No complex hierarchy beyond these primary roles is detailed in the current configuration, but could be implemented by customizing the filter logic or using more granular permissions.
5.  **Access Decision**: Based on token validity and role checks, the request is either allowed to proceed to the controller or denied with an appropriate HTTP status code (e.g., `401 Unauthorized`, `403 Forbidden`).

## User Account Management Security

This section details security aspects of user account verification and recovery processes.

### Email Verification Flow

Ensuring that a user's email address is valid and owned by them is crucial for account security and communication.

*   **Initiation:** The email verification process is typically initiated when a new user registers or when an existing user wishes to verify/change their email. An API call, such as `POST /api/public/emailSend` (as per `API_Documentation.md`), is made with the email address to be verified.
*   **Token Generation & Delivery:**
    *   Upon receiving the request, the backend generates a unique verification token (or code). While the exact generation mechanism (e.g., random string, JWT) is an implementation detail, it must be sufficiently random and hard to guess.
    *   This token is temporarily stored, often in a cache (like Redis) or a database table, associated with the user's email and an expiry timestamp.
    *   An email containing this verification token (or a clickable link embedding the token) is sent to the user's provided email address.
*   **Token Submission & Verification:**
    *   The user retrieves the token from their email and submits it back to the application, typically via an API call like `POST /api/public/emailCheck` (as per `API_Documentation.md`).
    *   The backend validates the submitted token against the stored one by checking for its existence, matching it with the user's email, and ensuring it has not expired.
*   **Outcome:**
    *   **Successful Verification:** If the token is valid, the user's email address is marked as verified. For new registrations, this might update the user's account status in the database (e.g., from "PENDING_VERIFICATION" to "ACTIVE"). The temporary verification token is then invalidated or deleted.
    *   **Failed Verification:** If the token is invalid, expired, or does not match, an error is returned to the user.
*   **Security Considerations:**
    *   **Token Expiry:** Verification tokens must have a limited validity period (e.g., 10-30 minutes) to reduce the risk of misuse if an email account is compromised.
    *   **One-Time Use:** Tokens should ideally be single-use to prevent replay attacks. Once used for verification, they should be invalidated.
    *   **Rate Limiting:** API endpoints for sending and verifying tokens should be rate-limited to prevent abuse (e.g., email flooding, brute-forcing tokens).

### Password Reset Flow

A secure password reset flow is essential for users who have forgotten their password.

*   **Initiation:**
    *   The user typically clicks a "Forgot Password" link on the login page.
    *   This action usually leads to a request to an API endpoint like `GET /api/public/findPassWithEmail` (as per `API_Documentation.md`), where the user submits their registered User ID (or email).
*   **Token Generation & Delivery:**
    *   Upon successful identification of the user, the backend generates a unique, cryptographically strong, and time-limited password reset token.
    *   This token is associated with the user's account and stored securely (e.g., hashed in the database or stored in a secure cache).
    *   An email is sent to the user's registered email address containing a unique link that includes this reset token.
*   **Token Usage & Password Update:**
    *   The user clicks the reset link in their email, which directs them to a password reset page in the application.
    *   The application captures the token from the URL.
    *   The user enters their new password and submits the form.
    *   The client sends the new password along with the reset token to a specific backend endpoint (e.g., a hypothetical `POST /api/public/resetPassword` - verify actual endpoint if available, or `UsersController.updatePassword` if it can be adapted for this, though that one seems to be for logged-in users). The `UserService.UpdateUserPassword(userId, temporalPassword)` is mentioned, which might be part of this flow internally if `temporalPassword` is the reset token or a temporary password generated after token validation. More commonly, an endpoint would take the reset token and the *new* password.
*   **Outcome:**
    *   **Successful Reset:** If the reset token is valid (exists, matches the user, not expired) and the new password meets complexity requirements, the backend updates the user's password in the database (storing a new hash). The password reset token is then invalidated. The user is notified of the successful password change and prompted to log in with their new password.
    *   **Failed Reset:** If the token is invalid, expired, or any step fails, an error message is displayed.
*   **Security Considerations:**
    *   **Token Security:** Reset tokens must be long, random, and unpredictable. They should be treated as sensitive credentials.
    *   **Token Expiry:** Tokens must have a short lifespan (e.g., 1-2 hours) to limit the window of opportunity for attackers.
    *   **One-Time Use:** Each token must be invalidated immediately after use.
    *   **Secure Transmission:** The reset link should be sent over HTTPS. The page where the user enters the new password must also be served over HTTPS.
    *   **Notification:** Inform the user via email when a password reset is initiated and when it is successfully completed. This helps detect unauthorized attempts.
    *   **Rate Limiting:** Protect against brute-force attacks on the token submission endpoint and the reset initiation endpoint.

## Token Refresh Mechanism

When an Access Token expires, the client can send a `POST` request to `/auth/api/protected/refresh` with the expired Access Token and the Refresh Token (sent via cookie). The `AuthorizationFilter` handles this request, validates the Refresh Token, and issues a new Access Token.
