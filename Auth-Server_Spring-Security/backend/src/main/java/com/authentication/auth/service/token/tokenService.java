package com.career_block.auth.service.token;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authentication.auth.DTO.token.tokenRefreshRequest;
import com.authentication.auth.configuration.token.jwtUtility;
import com.authentication.auth.service.redis.redisService;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class tokenService {


    private final jwtUtility jwtUtility;
    private final redisService redisService;

    @Transactional
    public ResponseEntity<?> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, tokenRefreshRequest request) throws IOException {

        String expiredToken = request.getExpiredToken();
        String provider = request.getProvider();

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
