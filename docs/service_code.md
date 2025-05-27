### **Service Codes and Responses (service_code.md)**

This document provides a high-level overview of common success and error response patterns used in the Emotion Diary project API.

#### **1. Success Responses**

Successful API requests will generally return standard HTTP success codes.

*   **`200 OK`**: Standard response for a successful GET request or a synchronous POST/PUT request that modifies a resource.
    *   **Example Body:** Typically the requested resource or a confirmation message.
    ```json
    // Example for GET /users/me
    {
      "userId": "uuid-1234-abcd",
      "email": "user@example.com",
      "name": "Test User",
      "role": "USER",
      "status": "ACTIVE"
    }
    ```

*   **`201 Created`**: Standard response for a successful POST request that results in the creation of a new resource.
    *   **Example Body:** Typically the newly created resource, or a link to it.
    ```json
    // Example for POST /diaries
    {
      "diaryId": "uuid-diary-5678",
      "title": "My First Entry",
      "content": "Today was a good day.",
      "emotion": "HAPPY",
      "createdAt": "YYYY-MM-DDTHH:mm:ssZ"
    }
    ```

*   **`204 No Content`**: Used when a request is successful, but there is no data to return (e.g., for a successful DELETE request).

#### **2. Error Responses**

Error responses aim to provide clear, consistent feedback. The project uses components like `ErrorController`, `CustomException`, and `ErrorResponse` to manage errors.

*   **Common Error Response Structure (based on `ErrorResponse`):**
    While the exact fields were not specified, a common structure might be:
    ```json
    {
      "timestamp": "YYYY-MM-DDTHH:mm:ssZ", // Time of the error
      "status": 400,                       // HTTP status code
      "error": "Bad Request",              // HTTP status message
      "message": "Specific error message about what went wrong.", // Developer-friendly or user-friendly message
      "path": "/api/endpoint",             // The path that was accessed
      "errorCode": "SPECIFIC_ERROR_CODE"   // Optional application-specific error code
    }
    ```

*   **General Categories of HTTP Error Codes Used:**

    *   **`400 Bad Request`**:
        *   **Description:** The server cannot or will not process the request due to something perceived to be a client error (e.g., malformed request syntax, invalid request message framing, or deceptive request routing).
        *   **Potential Reasons:** Missing required fields, invalid data types, validation errors (e.g., email format incorrect).
        *   **Example `errorCode` (hypothetical):** `VALIDATION_ERROR`, `MISSING_PARAMETER`

    *   **`401 Unauthorized`**:
        *   **Description:** The client request has not been completed because it lacks valid authentication credentials for the requested resource.
        *   **Potential Reasons:** Missing JWT, invalid JWT, expired JWT.
        *   **Example `errorCode` (hypothetical):** `UNAUTHENTICATED`, `TOKEN_EXPIRED`, `INVALID_TOKEN`

    *   **`403 Forbidden`**:
        *   **Description:** The server understood the request but refuses to authorize it. The user does not have the necessary permissions for the resource.
        *   **Potential Reasons:** User role does not permit access to the specific API or resource.
        *   **Example `errorCode` (hypothetical):** `ACCESS_DENIED`, `INSUFFICIENT_PERMISSIONS`

    *   **`404 Not Found`**:
        *   **Description:** The server has not found anything matching the Request-URI.
        *   **Potential Reasons:** Requesting a resource that does not exist (e.g., `/users/unknown-user-id`).
        *   **Example `errorCode` (hypothetical):** `RESOURCE_NOT_FOUND`

    *   **`409 Conflict`**:
        *   **Description:** The request could not be completed due to a conflict with the current state of the resource (e.g., trying to create a resource that already exists, like a user with an existing email).
        *   **Potential Reasons:** Duplicate entry.
        *   **Example `errorCode` (hypothetical):** `RESOURCE_CONFLICT`, `EMAIL_ALREADY_EXISTS`

    *   **`500 Internal Server Error`**:
        *   **Description:** The server encountered an unexpected condition that prevented it from fulfilling the request.
        *   **Potential Reasons:** Unhandled exceptions in the backend code.
        *   **Example `errorCode` (hypothetical):** `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`

*   **Note on Specificity:**
    This document provides a general overview. The actual `errorCode` values and detailed `message` content would be defined within the application's `CustomException` classes and their handling in the `ErrorController` or similar global exception handlers. Without direct source code access, a comprehensive list of every possible error code and message per endpoint cannot be provided.

