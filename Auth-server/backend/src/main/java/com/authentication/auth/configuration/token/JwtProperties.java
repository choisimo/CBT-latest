package com.authentication.auth.configuration.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.properties 파일의 'jwt' 접두사를 가진 설정들을 매핑하는 record 입니다.
 *
 * @param secretKey JWT 서명에 사용될 비밀키
 * @param accessTokenExpirationMinutes 액세스 토큰 만료 시간 (분)
 * @param refreshTokenExpirationMinutes 리프레시 토큰 만료 시간 (분)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secretKey,
        long accessTokenExpirationMinutes,
        long refreshTokenExpirationMinutes
) {
}
