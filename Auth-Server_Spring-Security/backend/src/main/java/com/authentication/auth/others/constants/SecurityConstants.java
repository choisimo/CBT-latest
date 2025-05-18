package com.authentication.auth.others.constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 보안 관련 상수를 정의하는 Enum 클래스
 * JWT 토큰, 인증, 인가 관련 상수 정의
 */
public enum SecurityConstants {

    TOKEN_TYPE("TOKEN_TYPE", "JWT"),
    TOKEN_HEADER("TOKEN_HEADER", "Authorization"),
    TOKEN_PREFIX("TOKEN_PREFIX", "Bearer "),
    TOKEN_TYPE_REFRESH("TOKEN_TYPE_REFRESH", "refreshJWT"),
    
    ACCESS_TOKEN_EXPIRATION("ACCESS_TOKEN_EXPIRATION", "1800"), // 30분(초 단위)
    REFRESH_TOKEN_EXPIRATION("REFRESH_TOKEN_EXPIRATION", "2592000"), // 30일(초 단위)
    COOKIE_NAME("COOKIE_NAME", "refresh_token"),
    COOKIE_SECURE("COOKIE_SECURE", "true"),
    COOKIE_HTTP_ONLY("COOKIE_HTTP_ONLY", "true"),
    COOKIE_PATH("COOKIE_PATH", "/"),
    COOKIE_DOMAIN("COOKIE_DOMAIN", ""),
    REMEMBER_ME_KEY("REMEMBER_ME_KEY", "rememberMeKey"),
    REMEMBER_ME_VALIDITY("REMEMBER_ME_VALIDITY", "1209600"), // 14일(초 단위)
    CORS_ALLOWED_ORIGINS("CORS_ALLOWED_ORIGINS", "*"),
    CORS_ALLOWED_METHODS("CORS_ALLOWED_METHODS", "GET,POST,PUT,DELETE,OPTIONS"),
    CSRF_HEADER_NAME("CSRF_HEADER_NAME", "X-CSRF-TOKEN"),
    CSRF_PARAMETER_NAME("CSRF_PARAMETER_NAME", "_csrf"),
    LOGIN_URL("LOGIN_URL", "/api/auth/login"),
    LOGOUT_URL("LOGOUT_URL", "/api/auth/logout"),
    SIGNUP_URL("SIGNUP_URL", "/api/auth/signup"),
    AUTH_WHITE_LIST("AUTH_WHITE_LIST", "/api/auth/**,/public/**,/api/v1/health,/swagger-ui/**,/v3/api-docs/**");
    
    private final String key;
    private final String value;
    
    // 값 탐색을 위한 매핑
    private static final Map<String, SecurityConstants> BY_KEY = new HashMap<>();
    private static final Map<String, SecurityConstants> BY_VALUE = new HashMap<>();
    
    // 정적 초기화 블록으로 매핑 초기화
    static {
        for (SecurityConstants constant : values()) {
            BY_KEY.put(constant.getKey(), constant);
            BY_VALUE.put(constant.getValue(), constant);
        }
    }
    
    /**
     * SecurityConstants 생성자
     * @param key 상수 키
     * @param value 상수 값
     */
    SecurityConstants(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            throw new IllegalArgumentException("키와 값은 null이거나 빈 문자열일 수 없습니다.");
        }
        this.key = key;
        this.value = value;
    }
    
    /**
     * 상수 키를 반환합니다.
     * @return 상수 키
     */
    public String getKey() {
        return key;
    }
    
    /**
     * 상수 값을 반환합니다.
     * @return 상수 값
     */
    public String getValue() {
        return value;
    }
    
    /**
     * 상수 값을 정수로 반환합니다.
     * @return 정수로 변환된 상수 값
     * @throws NumberFormatException 상수 값이 정수로 변환될 수 없는 경우
     */
    public int getIntValue() {
        return Integer.parseInt(value);
    }
    
    /**
     * 상수 값을 long으로 반환합니다.
     * @return long으로 변환된 상수 값
     * @throws NumberFormatException 상수 값이 long으로 변환될 수 없는 경우
     */
    public long getLongValue() {
        return Long.parseLong(value);
    }
    
    /**
     * 상수 값을 boolean으로 반환합니다.
     * @return boolean으로 변환된 상수 값
     */
    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 키로 SecurityConstants를 찾습니다.
     * @param key 찾으려는 상수의 키
     * @return 해당 키에 매핑된 SecurityConstants 또는 null
     */
    public static SecurityConstants fromKey(String key) {
        return BY_KEY.get(key);
    }
    
    /**
     * 값으로 SecurityConstants를 찾습니다.
     * @param value 찾으려는 상수의 값
     * @return 해당 값을 가진 SecurityConstants 또는 null
     */
    public static SecurityConstants fromValue(String value) {
        return BY_VALUE.get(value);
    }
    
    /**
     * 키로 SecurityConstants를 안전하게 찾습니다.
     * @param key 찾으려는 상수의 키
     * @return 해당 키에 매핑된 SecurityConstants를 담고 있는 Optional
     */
    public static Optional<SecurityConstants> getByKey(String key) {
        return Optional.ofNullable(fromKey(key));
    }
    
    /**
     * 값으로 SecurityConstants를 안전하게 찾습니다.
     * @param value 찾으려는 상수의 값
     * @return 해당 값을 가진 SecurityConstants를 담고 있는 Optional
     */
    public static Optional<SecurityConstants> getByValue(String value) {
        return Optional.ofNullable(fromValue(value));
    }
    
    /**
     * Enum 상수를 문자열로 반환합니다.
     * @return 키와 값을 포함한 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("%s[key=%s, value=%s]", name(), key, value);
    }
    
    /**
     * 모든 SecurityConstants를 문자열로 출력합니다.
     * 디버깅 용도로 유용합니다.
     * @return 모든 보안 상수에 대한 문자열 표현
     */
    public static String printAll() {
        StringBuilder sb = new StringBuilder("SecurityConstants:\n");
        for (SecurityConstants constant : values()) {
            sb.append(constant.toString()).append("\n");
        }
        return sb.toString();
    }
}
    }
}
