package com.authentication.auth.controller.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.service.oauth2.Oauth2Service;
import com.authentication.auth.service.redis.RedisService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth2")
public class Oauth2Controller {

    // 하핫.. 너는 서비스다...
    private final Oauth2Service oauth2Service;
    private final RedisService redisService;
    private final JwtUtility jwtUtility;

    @PostMapping("/callback/kakao")
    public ResponseEntity<?> oauth2KakaoLogin(@RequestBody Map<String, String> requestBody,
            HttpServletResponse response) {
        log.info("/oauth2/callback/kakao");
        return oauth2Service.handleOauth2Login(requestBody, response, "kakao");
    }

    @PostMapping("/callback/naver")
    public ResponseEntity<?> oauth2NaverLogin(@RequestBody Map<String, String> requestBody,
            HttpServletResponse response) {
        log.info("/oauth2/callback/naver");
        String state = UUID.randomUUID().toString();
        return oauth2Service.handleOauth2Login(requestBody, response, "naver");
    }

    @PostMapping("/callback/google")
    public ResponseEntity<?> oauth2GoogleLogin(@RequestBody Map<String, String> requestBody,
            HttpServletResponse response) {
        log.info("/oauth2/callback/google");
        log.info("tempCode for google from client server : {}", requestBody.get("tempCode"));
        return oauth2Service.handleOauth2Login(requestBody, response, "google");
    }

}
