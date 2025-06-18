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
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.redis.RedisService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collection;
// JwtUtility import removed as it's no longer directly used.

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenProvider tokenProvider;
    private final RedisService redisService;

    // 7 days in seconds
    private static final int REFRESH_TOKEN_TTL_SECONDS = SecurityConstants.REFRESH_TOKEN_TTL_SECONDS.getIntValue();

    public LoginResponse postLoginActions(Authentication authentication, HttpServletResponse response) {
        String userId = authentication.getName();
        // Extracting the first authority as role. Adapt if multiple roles or different logic is needed.
        String role = SecurityConstants.TOKEN_DEFAULT_ROLE.getValue(); 
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities != null && !authorities.isEmpty()) {
            GrantedAuthority firstAuthority = authorities.iterator().next();
            if (firstAuthority != null) {
                role = firstAuthority.getAuthority();
            }
        }

        String accessToken = tokenProvider.createAccessToken(userId, role);
        String refreshToken = tokenProvider.createRefreshToken(userId, "local"); // "local" for credentials-based login

        // Store the refresh token in Redis
        redisService.saveRToken(userId, "local", refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public ResponseEntity<?> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, TokenRefreshRequest request) throws IOException {

        String expiredAccessToken = request.expiredToken();
        String provider = request.provider();

        // 1. Extract Refresh Token from request body
        String refreshTokenFromBody = request.refreshToken();
        if (refreshTokenFromBody == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found in request body.");
        }

        // 2. Validate the Refresh Token
        /**
         *  Access Token :  Secure Storage 에 저장 -> Body 에서 가져오기
         *  Refresh Token : Secure Storage 에 저장 -> Body 에서 가져오기
         */
        if (!tokenProvider.validateRefreshToken(refreshTokenFromBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token.");
        }

        // 3. Extract User ID from the *expired* access token (it still contains user info)
        String userId = tokenProvider.getUserIdFromToken(expiredAccessToken);
        if (userId == null) {
            // This might happen if the expiredAccessToken is bogus or not what's expected
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not extract user ID from expired token.");
        }

        // 4. Check if the Refresh Token from request body exists in Redis for this user and provider
        if (!redisService.isRTokenExist(userId, provider, refreshTokenFromBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found in Redis or mismatch.");
        }

        // 5. If all checks pass, issue a new Access Token
        String newAccessToken = tokenProvider.refreshToken(expiredAccessToken, provider);

        // Note: The new access token is not stored in Redis here. Typically, only refresh tokens are stored.
        // The old refresh token in Redis remains valid until its own expiry or explicit revocation.
        // No new refresh token is issued in this flow by default by TokenProvider.refreshToken (which calls jwtUtility.refreshToken).

        return ResponseEntity.status(HttpStatus.OK).body(newAccessToken);
    }
}
