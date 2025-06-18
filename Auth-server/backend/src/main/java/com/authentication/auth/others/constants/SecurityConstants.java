package com.authentication.auth.others.constants;

import java.util.Arrays;
import java.util.List;

/**
 * 보안 관련 상수를 정의하는 열거형 클래스
 * 하드코딩 방지 및 중앙화된 상수 관리
 */
public enum SecurityConstants {
    // 토큰 관련 상수
    TOKEN_TYPE("JWT"),
    TOKEN_HEADER("Authorization"),
    TOKEN_PREFIX("Bearer "),
    REFRESH_TOKEN_TYPE("refreshJWT"),
    
    // 쿠키 관련 상수
    COOKIE_REFRESH_TOKEN("refreshToken"),
    COOKIE_ACCESS_TOKEN("access_token"),
    COOKIE_PATH("/"),
    
    // URL 패턴 관련 상수
    PUBLIC_API_PATH("/api/public/**"),
    AUTH_API_PATH("/api/auth/**"),
    LOGIN_PATH("/api/public/login"),
    REGISTER_PATH("/api/auth/register"),
    REFRESH_PATH("/api/auth/refresh"),
    ADMIN_API_PATH("/api/admin/**"),
    SWAGGER_UI_PATH("/swagger-ui/**"),
    API_DOCS_PATH("/v3/api-docs/**"),
    
    // 제공자 관련 상수
    DEFAULT_PROVIDER("server"),
    
    // 오류 메시지
    ERROR_INVALID_TOKEN("유효하지 않은 토큰입니다"),
    ERROR_TOKEN_EXPIRED("토큰이 만료되었습니다"),
    ERROR_NO_REFRESH_TOKEN("리프레시 토큰이 없습니다"),
    ERROR_NO_COOKIES("쿠키가 없습니다"),
    ERROR_ADMIN_REQUIRED("관리자 권한이 필요합니다"),
    ERROR_AUTHENTICATION_FAILED("인증에 실패했습니다");
    
    private final String value;
    
    SecurityConstants(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 공개 API 경로 목록 반환
     * @return 공개 API 경로 목록
     */
    public static List<String> getPublicPaths() {
        return Arrays.asList(
            PUBLIC_API_PATH.getValue(),
            LOGIN_PATH.getValue(),
            REGISTER_PATH.getValue(),
            REFRESH_PATH.getValue(),
            SWAGGER_UI_PATH.getValue(),
            API_DOCS_PATH.getValue()
        );
    }
    
    /**
     * 관리자 전용 API 경로 목록 반환
     * @return 관리자 전용 API 경로 목록
     */
    public static List<String> getAdminPaths() {
        return Arrays.asList(
            ADMIN_API_PATH.getValue()
        );
    }
}
