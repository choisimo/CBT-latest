package com.authentication.auth.token;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.authentication.auth.DTO.token.tokenDto;
import com.authentication.auth.configuration.token.jwtUtility;

import java.util.Collections;

public class JwtUtilityTest {

    @Test
    void testShortLivedAccessToken() throws InterruptedException {
        // 만료 시간 10초로 설정
        jwtUtility jwtUtility = new jwtUtility(
                "eyJraWQiOiJlZjdlY2JkMy0xODcyLTRkMGUtYjYyZC03NzJkZjU2ZDcyMjEiLCJhbGciOiJSUzUxMiJ9.ew0KICAic3ViIjogIjExMzI0M2ZkaDRzZGZoMCIsDQogICJuYW1lIjogImFzZGFzZGcxMjROYXQ0MzUzNGgiLA0KICAiaWF0IjogMTUxNjEyNDVmZ2g5MDIyZXINCn0.cvsTbjRw-DWUQcrgacNmpBFzSYO8rjEvY6oMlTFcicizb1VFVVgGPf1wOJHwkc09rxzmExD7wC2q9WG_VVQ05lqzTUUJ_OVUxiJ2KNHPL3ysvpCQH5i70zCoqkCNqTu_-WHF09HXV_VEZNsGBSHQokOGqdZr8cdpSVeo2Y2u2Bx_LKf7j6XbW0xL_QJeV1c1GZt6El1lbC01tptfLYc43KGhW7fpktxbyuPito3QCx7oYgi4IESABpDWGNAVTidt1v-TE-cEhoo8D5sv6zlAR49M-8ITj8BoRJdTqb-v85d2K-jJaG10bjRQxN16LphaD5vFNb7LvcyJdre15HTYJw",  // 32바이트 이상의 문자열
                "eyJlZjdlY2JkMy0xODcyLTRkMGUtYjYQ1MTYtODU0My0zYjM3ZWNhYzViM2EiLCJhbGciOiJSUzUxMiJ9.ew0KICAic3ViIjogIjExMzI0M2ZkZ2pnZmpkZmgwIiwNCiAgIm5hbWUiOiAiYTEyM3NkNDU3OTg5amhtZGdmaGpqNE5hdDQzNTM0aCIsDQogICJpYXQiOiAxNTE2MTI0NWZnamRnaHlqa2R5Z2ZnaGc5MGRmZ2gyMmVyDQp9.f0XpFz2rJ97kW4-jEodL5jQ79Tre5tY3sScViI6b9m3grk5nZm8EQd1wmu6u55ckageOvVLqg1SaWpqKTgIL6Dknv6Bh4K-kOEuHl5S6WzxNSyIk4B-dYu4n644ZVuhF54UeFoRfRjQ1kB2RfqXi_ekwtc4Q2ff-KU15EtEV_AN4R_gerSzC7VAwqj9BN4G1rbHTvSLsozcsGi4r1aSihXpaq4nrkad48TpLVSbkGWovdaVuR4aY9RARHDPvm5x1WG8bjaDMu7PB5at2LAayvd3yXRF9Xjr5MypX9AUg6Ne0VodeppFwah-PRjob8-5hb3l1yqTU8Ht6ml-dVO7UVw",      // 32바이트 이상의 문자열
                10L,  // ACCESS_TOKEN_VALIDITY (10초)
                3600L // REFRESH_TOKEN_VALIDITY (1시간)
        );

        // 유저 정보 설정
        String userId = "admin";
        String nickname = "관리자";
        SimpleGrantedAuthority role = new SimpleGrantedAuthority("ROLE_USER");

        // 토큰 생성
        tokenDto token = jwtUtility.buildToken(userId, nickname, Collections.singletonList(role));

        System.out.println("Generated Access Token: " + token.getAccessToken());
        System.out.println("Generated Refresh Token: " + token.getRefreshToken());

        // 생성된 토큰을 바로 검증 (유효해야 함)
        boolean isValid = jwtUtility.validateJWT(token.getAccessToken());
        System.out.println("Access Token is valid: " + isValid);

        // 10초 대기 후 토큰 만료 여부 검증
        System.out.println("Waiting for 10 seconds...");
        Thread.sleep(10000);

        // 만료된 토큰을 검증 (유효하지 않아야 함)
        isValid = jwtUtility.validateJWT(token.getAccessToken());
        System.out.println("Access Token is valid after 10 seconds: " + isValid);
    }
}
