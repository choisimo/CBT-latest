package com.authentication.auth.controller.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.configuration.oauth2.OauthProperties;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;
import com.authentication.auth.service.oauth2.Oauth2Service;
import com.authentication.auth.service.redis.RedisService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/oauth2")
public class Oauth2Controller {

    // 하핫.. 너는 서비스다...
    private final Oauth2Service oauth2Service;
    private final RedisService redisService;
    private final OauthProperties oauthProperties;
    private final JwtUtility jwtUtility;

    @PostMapping("/callback/kakao")
    @Deprecated(since = "2.0", forRemoval = true)
    public ResponseEntity<?> oauth2KakaoLogin(@RequestBody Map<String, String> requestBody,
            HttpServletResponse response) {
        log.info("/oauth2/callback/kakao - Legacy endpoint, use /callback/kakao instead");
        return oauth2LoginCallback("kakao", requestBody, response);
    }

    @PostMapping("/callback/naver")
    @Deprecated(since = "2.0", forRemoval = true)
    public ResponseEntity<?> oauth2NaverLogin(@RequestBody Map<String, String> requestBody,
            HttpServletResponse response) {
        log.info("/oauth2/callback/naver - Legacy endpoint, use /callback/naver instead");
        return oauth2LoginCallback("naver", requestBody, response);
    }

    @PostMapping("/callback/google")
    @Deprecated(since = "2.0", forRemoval = true)
    public ResponseEntity<?> oauth2GoogleLogin(@RequestBody Map<String, String> requestBody,
            HttpServletResponse response) {
        log.info("/oauth2/callback/google - Legacy endpoint, use /callback/google instead");
        return oauth2LoginCallback("google", requestBody, response);
    }

    @GetMapping("/login_url/{provider}")
    public ResponseEntity<Map<String, String>> getOAuth2LoginUrl(@PathVariable("provider") String provider) {
        String loginUrl;
        String state = null;
        
        switch (provider.toLowerCase()) {
            case "kakao" -> {
                loginUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code" +
                        "&client_id=" + oauthProperties.kakao().clientId() +
                        "&redirect_uri=" + UriUtils.encode(oauthProperties.kakao().redirectUri(), StandardCharsets.UTF_8);
            }
            case "naver" -> {
                // CSRF 방지를 위한 state 파라미터 생성 및 Redis 저장
                state = UUID.randomUUID().toString();
                redisService.save("oauth_state:" + state, "naver", 300); // 5분 만료
                loginUrl = "https://nid.naver.com/oauth2.0/authorize?response_type=code" +
                        "&client_id=" + oauthProperties.naver().clientId() +
                        "&redirect_uri=" + UriUtils.encode(oauthProperties.naver().redirectUri(), StandardCharsets.UTF_8) +
                        "&state=" + state;
            }
            case "google" -> {
                loginUrl = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code" +
                        "&client_id=" + oauthProperties.google().clientId() +
                        "&redirect_uri=" + UriUtils.encode(oauthProperties.google().redirectUri(), StandardCharsets.UTF_8) +
                        "&scope=openid%20profile%20email";
            }
            default -> throw new CustomException(ErrorType.UNSUPPORTED_OAUTH_PROVIDER, "지원하지 않는 OAuth 제공자입니다.");
        }
        
        Map<String, String> response = Map.of(
            "login_url", loginUrl,
            "provider", provider.toLowerCase(),
            "state", state != null ? state : ""
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * React Native용 통합 OAuth2 콜백 엔드포인트
     */
    @PostMapping("/callback/{provider}")
    public ResponseEntity<?> oauth2LoginCallback(@PathVariable("provider") String provider,
                                                @RequestBody Map<String, String> requestBody,
                                                HttpServletResponse response) {
        log.info("/oauth2/callback/{} - React Native", provider);
        log.info("Request body: {}", requestBody);
        
        // state 검증 (네이버의 경우)
        if ("naver".equals(provider.toLowerCase()) && requestBody.containsKey("state")) {
            String state = requestBody.get("state");
            String savedProvider = redisService.get("oauth_state:" + state);
            if (!"naver".equals(savedProvider)) {
                throw new CustomException(ErrorType.INVALID_OAUTH_STATE, "잘못된 OAuth state 파라미터입니다.");
            }
            redisService.delete("oauth_state:" + state); // 사용 후 삭제
        }
        
        var loginResponse = oauth2Service.handleOauth2Login(requestBody, response, provider);
        return ResponseEntity.ok(loginResponse);
    }

}
