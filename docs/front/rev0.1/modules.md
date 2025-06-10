# Utility Modules Documentation

This document provides details about various utility modules used within the application, including configuration constants and helper functions.

---

## 1. `oauthConfig.ts`

-   **Module Name**: `oauthConfig.ts`
-   **Path**: `CBT-front/src/auth/oauthConfig.ts`
-   **Purpose**: This module provides configuration objects for Naver and Kakao OAuth providers. These configurations are used for integrating social login functionalities within the application, likely with a library like `react-native-app-auth`. The actual client IDs, secrets, and redirect URIs are expected to be loaded from environment variables via `react-native-config`.

-   **Exported Members**:

    -   **`NAVER_CONFIG`** (Constant)
        -   **Value Structure**:
            ```typescript
            {
              clientId: string, // Naver Client ID from Config.NAVER_CLIENT_ID
              clientSecret: string, // Naver Client Secret from Config.NAVER_CLIENT_SECRET
              redirectUrl: string, // Redirect URI from Config.REDIRECT_URI
              scopes: string[], // e.g., ['profile']
              serviceConfiguration: {
                authorizationEndpoint: string, // e.g., 'https://nid.naver.com/oauth2.0/authorize'
                tokenEndpoint: string, // e.g., 'https://nid.naver.com/oauth2.0/token'
              }
            }
            ```
        -   **Purpose**: Contains all necessary configuration details for Naver OAuth authentication, including client identifiers, redirect URI, requested scopes, and service endpoints for authorization and token exchange.

    -   **`KAKAO_CONFIG`** (Constant)
        -   **Value Structure**:
            ```typescript
            {
              clientId: string, // Kakao Client ID from Config.KAKAO_CLIENT_ID
              redirectUrl: string, // Redirect URI from Config.REDIRECT_URI
              scopes: string[], // e.g., ['account_email', 'profile_nickname']
              serviceConfiguration: {
                authorizationEndpoint: string, // e.g., 'https://kauth.kakao.com/oauth/authorize'
                tokenEndpoint: string, // e.g., 'https://kauth.kakao.com/oauth/token'
              }
            }
            ```
        -   **Purpose**: Contains all necessary configuration details for Kakao OAuth authentication. Similar to `NAVER_CONFIG`, it includes client ID, redirect URI, scopes, and service endpoints. Note that `clientSecret` is not typically required for Kakao's mobile OAuth flow on the client side.

---

## 2. `api.ts`

-   **Module Name**: `api.ts`
-   **Path**: `CBT-front/src/constants/api.ts`
-   **Purpose**: This module centralizes the base URL for the application's backend API. This allows for easy updates to the API endpoint across the application.

-   **Exported Members**:

    -   **`BASIC_URL`** (Constant)
        -   **Value**: `'https://your-api-domain.com'` (String)
        -   **Purpose**: Defines the base domain for all API requests made by the application. Other parts of the application will use this constant to construct full API endpoint URLs. The current value appears to be a placeholder and would need to be replaced with the actual API domain for a production environment.

---
