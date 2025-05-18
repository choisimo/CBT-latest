package com.authentication.auth.constants;

import org.springframework.http.HttpStatus;

/**
 * 오류 유형을 정의하는 열거형
 */
public enum ErrorType {
    AUTHENTICATION_FAILED("인증 실패", "사용자 인증에 실패했습니다", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("유효하지 않은 토큰", "제공된 토큰이 유효하지 않습니다", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("토큰 만료", "토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("접근 거부", "해당 리소스에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN),
    INVALID_REQUEST("잘못된 요청", "요청 형식이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("리소스 없음", "요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    SERVER_ERROR("서버 오류", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
    
    private final String title;
    private final String message;
    private final HttpStatus status;
    
    ErrorType(String title, String message, HttpStatus status) {
        this.title = title;
        this.message = message;
        this.status = status;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
    
    public int getStatusCode() {
        return status.value();
    }
}
