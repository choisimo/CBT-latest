package com.authentication.auth.service.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SnsTokenValidator {

    @Value("${kakao.token.validate.url}")
    private String kakaoValidateURL;

    @Value("${naver.token.validate.url}")
    private String naverValidateURL;

    @Value("${google.token.validate.url}")
    private String googleValidateURL;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String naverRedirectUri;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${site.domain}")
    private String domain;

    private final RestTemplate restTemplate;

    public SnsTokenValidator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateToken(String token, String provider) {
        log.info("provider : [{}] token validation start", provider);
        return switch (provider) {
            case "kakao" -> kakaoValidateToken(token);
            case "naver" -> naverValidateToken(token);
            case "google" -> googleValidateToken(token);
            default -> false;
        };
    }

    public Map<String, String> getNewTokenByRefreshToken(String refreshToken, String provider) {
        String url = null;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        switch (provider) {
            case "kakao":
                url = "https://kauth.kakao.com/oauth/token";
                params.add("grant_type", "refresh_token");
                params.add("client_id", kakaoClientId);
                params.add("refresh_token", refreshToken);
                params.add("client_secret", kakaoClientSecret);
                break;
            case "naver":
                url = "https://nid.naver.com/oauth2.0/token";
                params.add("grant_type", "refresh_token");
                params.add("client_id", naverClientId);
                params.add("refresh_token", refreshToken);
                params.add("client_secret", naverClientSecret);
                break;
            case "google":
                url = "https://oauth2.googleapis.com/token";
                params.add("grant_type", "refresh_token");
                params.add("client_id", googleClientId);
                params.add("refresh_token", refreshToken);
                params.add("client_secret", googleClientSecret);
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", response.getBody().get("access_token").toString());
            if (response.getBody().containsKey("refresh_token")) {
                tokens.put("refresh_token", response.getBody().get("refresh_token").toString());
            }
            return tokens;
        } else {
            throw new RuntimeException("Failed to get sns tokens for provider: " + provider);
        }
    }

    private boolean kakaoValidateToken(String token) {
        String url = kakaoValidateURL;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("카카오 토큰 검증 중 에러 발생", e);
            return false;
        }
    }

    private boolean naverValidateToken(String token) {
        String url = naverValidateURL;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("네이버 토큰 검증 중 에러 발생", e);
            return false;
        }
    }

    private boolean googleValidateToken(String token) {
        String url = googleValidateURL + "?id_token=" + token;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("구글 토큰 검증 중 에러 발생", e);
            return false;
        }
    }
}
