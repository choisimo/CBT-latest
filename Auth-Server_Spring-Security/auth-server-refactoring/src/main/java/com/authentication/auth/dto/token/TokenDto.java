package com.authentication.auth.dto.token;

/**
 * 토큰 정보를 담는 불변 레코드
 * @param accessToken 액세스 토큰
 * @param refreshToken 리프레시 토큰
 * @param expiresIn 액세스 토큰 만료 시간(초)
 * @param tokenType 토큰 타입
 */
public record TokenDto(
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType
) {
    /**
     * 기본 토큰 생성 팩토리 메서드
     */
    public static TokenDto of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenDto(accessToken, refreshToken, expiresIn, "Bearer");
    }
    
    /**
     * 액세스 토큰만 포함하는 DTO 생성
     */
    public static TokenDto accessTokenOnly(String accessToken, long expiresIn) {
        return new TokenDto(accessToken, null, expiresIn, "Bearer");
    }
}
