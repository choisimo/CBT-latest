// package com.authentication.auth.exception;

// public enum ErrorType {
//     // General Errors
//     GENERAL_ERROR("General error", 400),
//     INVALID_REQUEST("Invalid request", 400),
//     UNAUTHORIZED("Unauthorized", 401),
//     FORBIDDEN("Forbidden", 403),
//     NOT_FOUND("Resource not found", 404),
//     INTERNAL_SERVER_ERROR("Internal server error", 500),

//     // Authentication Errors
//     AUTHENTICATION_FAILED("Authentication failed", 401),
//     INVALID_TOKEN("Invalid token", 401),
//     EXPIRED_TOKEN("Expired token", 401),
//     REFRESH_TOKEN_NOT_FOUND("Refresh token not found", 401),
//     REFRESH_TOKEN_EXPIRED("Refresh token expired", 401),
//     INVALID_CREDENTIALS("Invalid credentials", 401),

//     // User Errors
//     USER_NOT_FOUND("User not found", 404),
//     EMAIL_ALREADY_EXISTS("Email already exists", 409),
//     USERNAME_ALREADY_EXISTS("Username already exists", 409),
//     NICKNAME_ALREADY_EXISTS("Nickname already exists", 409),
//     INVALID_EMAIL_CODE("Invalid email verification code", 401),

//     // File Upload Errors
//     INVALID_FILE_NAME("Invalid file name", 400),
//     INVALID_FILE_EXTENSION("Invalid file extension", 400),
//     INVALID_FILE_CONTENT("Invalid file content", 400),
//     FILE_UPLOAD_FAILED("File upload failed", 500),
//     FILE_TOO_LARGE("File size exceeds maximum limit", 400),
//     EMPTY_FILE("File is empty", 400),

//     // Other specific errors
//     EMAIL_SEND_FAILURE("Failed to send email", 500);

//     private final String message;
//     private final int statusCode;

//     ErrorType(String message, int statusCode) {
//         this.message = message;
//         this.statusCode = statusCode;
//     }

//     public String getMessage() {
//         return message;
//     }

//     public int getStatusCode() {
//         return statusCode;
//     }
// }


package com.authentication.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
    // General Errors
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED_ACTION(HttpStatus.UNAUTHORIZED, "권한이 없는 작업입니다."),
    FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "금지된 작업입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // File specific errors
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "업로드된 파일이 비어있습니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "파일 이름이 유효하지 않습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 확장자입니다."),
    INVALID_FILE_CONTENT(HttpStatus.BAD_REQUEST, "파일 내용이 유효하지 않거나 손상되었습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),
    MAX_FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 너무 큽니다."),

    // User specific errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠겼습니다."),
    
    // Token specific errors
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "리프레시 토큰을 찾을 수 없습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),

    // OAuth2 specific errors
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAuth2 인증에 실패했습니다."),
    OAUTH2_PROVIDER_MISMATCH(HttpStatus.BAD_REQUEST, "OAuth2 제공자가 일치하지 않습니다."),
    
    // Diary specific errors
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "다이어리를 찾을 수 없습니다."),
    DIARY_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "다이어리 생성에 실패했습니다."),
    DIARY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "다이어리 수정에 실패했습니다."),
    DIARY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "다이어리 삭제에 실패했습니다."),
    
    // AI Service specific errors
    AI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석 서비스 호출에 실패했습니다."),

    // Backward compatibility legacy constants
    GENERAL_ERROR(HttpStatus.BAD_REQUEST, "General error"),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Username already exists"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Expired token"),
    TOKEN_EXPIRED_ALIAS(HttpStatus.UNAUTHORIZED, "Token expired"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Authentication failed"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds maximum limit");


    private final HttpStatus status;
    private final String message;

    ErrorType(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Returns the integer HTTP status code associated with this error type.
     */
    public int getStatusCode() {
        return status.value();
    }
}
