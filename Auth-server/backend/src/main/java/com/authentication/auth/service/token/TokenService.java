package com.authentication.auth.service.token;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.service.redis.RedisService;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtility jwtUtility;
    private final RedisService redisService;

    @Transactional
    public ResponseEntity<?> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, TokenRefreshRequest request) throws IOException {

        String expiredToken = request.expiredToken();
        String provider = request.provider();

        if (jwtUtility.validateJWT(expiredToken)) {
            // token 갱신
            String newToken = jwtUtility.refreshToken(expiredToken);

            // Redis에 갱신된 토큰 저장
            String userId = jwtUtility.getUserIdFromToken(newToken);

            String RToken = jwtUtility.checkCookie(httpRequest, httpResponse, provider);
            if(!redisService.isRTokenExist(userId, provider, RToken)){
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("There is no refresh token in REDIS");
            }

            return ResponseEntity.status(HttpStatus.OK).body(newToken);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("not a valid token");
        }
    }
}
