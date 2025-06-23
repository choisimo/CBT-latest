package com.authentication.auth.token;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.configuration.token.JwtProperties;
import com.authentication.auth.configuration.token.JwtUtility;

import java.util.Collections;

public class JwtUtilityTest {

    @Test
    void testShortLivedAccessToken() throws InterruptedException {
        // JwtProperties 객체 생성
        JwtProperties jwtProperties = new JwtProperties(
                "testSecretKeytestSecretKeytestSecretKeytestSecretKeytestSecretKey",
                "testSecretKey2testSecretKey2testSecretKey2testSecretKey2testSecretKey2",
                "testAuthorizedKeytestAuthorizedKeytestAuthorizedKeytestAuthorizedKey",
                3600000L, // 1 hour
                86400000L  // 24 hours
        );

        // 만료 시간 10초로 설정
        JwtUtility jwtUtility = new JwtUtility(jwtProperties);

        // 유저 정보 설정
        String userId = "admin";
        SimpleGrantedAuthority role = new SimpleGrantedAuthority("ROLE_USER");

        // 토큰 생성
        TokenDto token = jwtUtility.buildToken(userId, Collections.singletonList(role));

        System.out.println("Generated Access Token: " + token.accessToken());
        System.out.println("Generated Refresh Token: " + token.refreshToken());

        // 생성된 토큰을 바로 검증 (유효해야 함)
        boolean isValid = jwtUtility.validateJWT(token.accessToken());
        System.out.println("Access Token is valid: " + isValid);

        // 10초 대기 후 토큰 만료 여부 검증
        System.out.println("Waiting for 10 seconds...");
        Thread.sleep(10000);

        // 만료된 토큰을 검증 (유효하지 않아야 함)
        isValid = jwtUtility.validateJWT(token.accessToken());
        System.out.println("Access Token is valid after 10 seconds: " + isValid);
    }
}
