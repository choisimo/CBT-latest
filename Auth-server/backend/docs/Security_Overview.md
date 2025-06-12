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
4.  **Role-Based Access**: If the token is valid, the filter checks if the user's roles (extracted from the JWT) grant access to the requested resource. For example, access to `/api/admin/**` paths requires an `ADMIN` role.
5.  **Access Decision**: Based on token validity and role checks, the request is either allowed to proceed to the controller or denied with an appropriate HTTP status code (e.g., `401 Unauthorized`, `403 Forbidden`).

## Token Refresh Mechanism

When an Access Token expires, the client can send a `POST` request to `/auth/api/protected/refresh` with the expired Access Token and the Refresh Token (sent via cookie). The `AuthorizationFilter` handles this request, validates the Refresh Token, and issues a new Access Token.
