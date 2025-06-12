package com.authentication.auth.configuration.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.properties 파일의 'spring.security.oauth2.client.registration' 접두사를 가진
 * 모든 소셜 로그인 설정들을 매핑하는 record 입니다.
 *
 * @param kakao 카카오 설정
 * @param naver 네이버 설정
 * @param google 구글 설정
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration")
public record OauthProperties(
        Client kakao,
        Client naver,
        Client google
) {
    /**
     * 각 소셜 로그인 제공자의 공통 설정값을 담는 record 입니다.
     *
     * @param clientId 클라이언트 ID
     * @param clientSecret 클라이언트 시크릿
     * @param redirectUri 리다이렉트 URI
     */
    public record Client(
            String clientId,
            String clientSecret,
            String redirectUri
    ) {}
}
