package com.authentication.auth.filter;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 보안 필터 순서 상수
 * @Details: 필터 체인 내에서 필터의 실행 순서를 정의하는 열거형
 */
public enum SecurityFilterOrder {
    
    // 값이 낮을수록 먼저 실행됨
    REQUEST_LOGGING_FILTER(50),
    CORS_FILTER(100),
    CSRF_FILTER(200),
    AUTHENTICATION_FILTER(300),
    JWT_VERIFICATION_FILTER(400),
    AUTHORIZATION_FILTER(500),
    SESSION_MANAGEMENT_FILTER(600),
    EXCEPTION_TRANSLATION_FILTER(700);
    
    private final int order;
    
    SecurityFilterOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return order;
    }
}
