package com.authentication.auth.constants;

/**
 * 필터 실행 순서를 정의하는 열거형
 * 값이 낮을수록 먼저 실행됨
 */
public enum FilterOrder {
    REQUEST_LOGGING(50),
    CORS(100),
    CSRF(200),
    AUTHENTICATION(300),
    JWT_VERIFICATION(400),
    AUTHORIZATION(500),
    SESSION_MANAGEMENT(600),
    EXCEPTION_TRANSLATION(700),
    SNS_REQUEST(800);
    
    private final int order;
    
    FilterOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return order;
    }
}
