package com.authentication.auth.service.token;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.dto.token.TokenRefreshResponse;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.redis.RedisService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collection;

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
        // Refresh Token을 HttpOnly 쿠키로 설정하는 로직은 Controller 또는 AuthController에서 담당 (P2 단계에서 진행 예정)
        // tokenProvider.setHttpOnlyCookie(response, refreshToken); 

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenRefreshResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, TokenRefreshRequest request) throws IOException {

        String expiredAccessToken = request.expiredToken();
        String provider = request.provider();

        // 1. Extract Refresh Token from request body
        String refreshTokenFromBody = request.refreshToken();
        if (refreshTokenFromBody == null || refreshTokenFromBody.isBlank()) {
            throw new CustomException(ErrorType.REFRESH_TOKEN_NOT_FOUND, "요청 본문에 리프레시 토큰이 없습니다.");
        }

        // 2. Validate the Refresh Token
        if (!tokenProvider.validateRefreshToken(refreshTokenFromBody)) {
            throw new CustomException(ErrorType.INVALID_REFRESH_TOKEN, "유효하지 않거나 만료된 리프레시 토큰입니다.");
        }

        // 3. Extract User ID from the *expired* access token (it still contains user info)
        String userId = tokenProvider.getUserIdFromToken(expiredAccessToken);
        if (userId == null) {
            // This might happen if the expiredAccessToken is bogus or not what's expected
            throw new CustomException(ErrorType.INVALID_ACCESS_TOKEN, "만료된 액세스 토큰에서 사용자 ID를 추출할 수 없습니다.");
        }

        // 4. Check if the Refresh Token from request body exists in Redis for this user and provider
        if (!redisService.isRTokenExist(userId, provider, refreshTokenFromBody)) {
            throw new CustomException(ErrorType.REFRESH_TOKEN_MISMATCH, "리프레시 토큰이 Redis에 없거나 일치하지 않습니다.");
        }

        // 5. If all checks pass, issue a new Access Token
        String newAccessToken = tokenProvider.refreshToken(expiredAccessToken, provider);
        if (newAccessToken == null || newAccessToken.isBlank()) {
            throw new CustomException(ErrorType.TOKEN_CREATION_FAILED, "새로운 액세스 토큰 생성에 실패했습니다.");
        }

        // Note: The new access token is not stored in Redis here. Typically, only refresh tokens are stored.
        // The old refresh token in Redis remains valid until its own expiry or explicit revocation.
        // No new refresh token is issued in this flow by default by TokenProvider.refreshToken (which calls jwtUtility.refreshToken).

        return new TokenRefreshResponse(newAccessToken);
    }
}
