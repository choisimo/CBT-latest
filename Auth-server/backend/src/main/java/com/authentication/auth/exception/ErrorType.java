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
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."), // OK
    UNAUTHORIZED_ACTION(HttpStatus.UNAUTHORIZED, "권한이 없는 작업입니다."), // OK
    FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "금지된 작업입니다."), // OK
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."), // OK
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."), // OK

    // File specific errors
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "업로드된 파일이 비어있습니다."), // OK
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "파일 이름이 유효하지 않습니다."), // OK
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 확장자입니다."), // OK
    INVALID_FILE_CONTENT(HttpStatus.BAD_REQUEST, "파일 내용이 유효하지 않거나 손상되었습니다."), // OK
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."), // OK
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."), // OK
    MAX_FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 너무 큽니다."), // OK (Replaces FILE_TOO_LARGE)

    // User specific errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."), // OK
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."), // OK
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 닉네임입니다."), // OK
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 사용자 ID입니다."), // Renamed from "Username already exists" for clarity
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."), // OK
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠겼습니다."), // OK
    INVALID_EMAIL_CODE(HttpStatus.UNAUTHORIZED, "이메일 인증 코드가 유효하지 않습니다."), // Added from old version

    // Token specific errors
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."), // OK (Replaces EXPIRED_TOKEN, TOKEN_EXPIRED_ALIAS)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."), // OK
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "리프레시 토큰을 찾을 수 없습니다."), // OK
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."), // OK

    // OAuth2 specific errors
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAuth2 인증에 실패했습니다."), // OK
    OAUTH2_PROVIDER_MISMATCH(HttpStatus.BAD_REQUEST, "OAuth2 제공자가 일치하지 않습니다."), // OK
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "잘못된 OAuth state 파라미터입니다."), // NEW
    
    // Diary specific errors
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "다이어리를 찾을 수 없습니다."), // OK
    DIARY_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "다이어리 생성에 실패했습니다."), // OK
    DIARY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "다이어리 수정에 실패했습니다."), // OK
    DIARY_NOT_FOUND_FOR_AI_UPDATE(HttpStatus.NOT_FOUND, "AI 업데이트용 다이어리를 찾을 수 없습니다."),
    DIARY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "다이어리 삭제에 실패했습니다."), // OK
    
    // AI Service specific errors
    AI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석 서비스 호출에 실패했습니다."), // OK

    // Other/Legacy - Review if these are still needed or can be mapped to existing ones
    GENERAL_ERROR(HttpStatus.BAD_REQUEST, "일반 오류가 발생했습니다."), // Changed message to Korean
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."), // Changed message to Korean
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근이 금지되었습니다."), // Changed message to Korean
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문이 유효하지 않습니다."), // Added for more specific client errors
    EMAIL_TEMPLATE_LOAD_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 템플릿 로드에 실패했습니다."),
    INVALID_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 유효하지 않습니다."),
    OAUTH2_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "OAuth2 응답이 유효하지 않습니다."),
    OAUTH2_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAuth2 제공자 오류가 발생했습니다."),
    OAUTH2_INVALID_GRANT(HttpStatus.BAD_REQUEST, "구글 인증 실패 (invalid_grant)"),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 리프레시 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 액세스 토큰에서 사용자 ID를 추출할 수 없습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다."),
    TOKEN_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 생성에 실패했습니다."),
    EMAIL_SEND_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."); // Added from old version


    private final HttpStatus httpStatus;
    private final String message;

    ErrorType(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getStatusCode() {
        return this.httpStatus.value();
    }

    public String getMessage() {
        return this.message;
    }
}
