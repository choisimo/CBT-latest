package com.authentication.auth.exception;

public enum ErrorType {
    // General Errors
    INVALID_REQUEST("Invalid request", 400),
    UNAUTHORIZED("Unauthorized", 401),
    FORBIDDEN("Forbidden", 403),
    NOT_FOUND("Resource not found", 404),
    INTERNAL_SERVER_ERROR("Internal server error", 500),

    // Authentication Errors
    AUTHENTICATION_FAILED("Authentication failed", 401),
    INVALID_TOKEN("Invalid token", 401),
    EXPIRED_TOKEN("Expired token", 401),
    REFRESH_TOKEN_NOT_FOUND("Refresh token not found", 401),
    REFRESH_TOKEN_EXPIRED("Refresh token expired", 401),
    INVALID_CREDENTIALS("Invalid credentials", 401),

    // User Errors
    USER_NOT_FOUND("User not found", 404),
    EMAIL_ALREADY_EXISTS("Email already exists", 409),
    USERNAME_ALREADY_EXISTS("Username already exists", 409),
    NICKNAME_ALREADY_EXISTS("Nickname already exists", 409),
    INVALID_EMAIL_CODE("Invalid email verification code", 401),

    // File Upload Errors
    INVALID_FILE_NAME("Invalid file name", 400),
    INVALID_FILE_EXTENSION("Invalid file extension", 400),
    INVALID_FILE_CONTENT("Invalid file content", 400),
    FILE_UPLOAD_FAILED("File upload failed", 500),
    FILE_TOO_LARGE("File size exceeds maximum limit", 400),
    EMPTY_FILE("File is empty", 400),

    // Other specific errors
    EMAIL_SEND_FAILURE("Failed to send email", 500);

    private final String message;
    private final int statusCode;

    ErrorType(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
