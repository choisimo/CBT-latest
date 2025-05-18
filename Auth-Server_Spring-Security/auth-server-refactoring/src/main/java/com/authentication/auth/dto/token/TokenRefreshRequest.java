package com.authentication.auth.dto.token;

/**
 * 토큰 갱신 요청 정보를 담는 불변 레코드
 * @param expiredToken 만료된 토큰
 * @param provider 인증 제공자
 */
public record TokenRefreshRequest(
    String expiredToken,
    String provider
) {
    /**
     * 유효성 검사
     * @return 유효 여부
     */
    public boolean isValid() {
        return expiredToken != null && !expiredToken.isBlank() && 
               provider != null && !provider.isBlank();
    }
}
