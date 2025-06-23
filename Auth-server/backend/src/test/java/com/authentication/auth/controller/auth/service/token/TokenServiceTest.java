package com.authentication.auth.controller.auth.service.token;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.dto.token.TokenRefreshResponse;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.service.token.TokenProvider;
import com.authentication.auth.service.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private RedisService redisService;
    @InjectMocks
    private TokenService tokenService;
    @Mock
    private Authentication authentication;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;

    private final String testLoginId = "testUser";
    private final String testEmail = "test@example.com";
    private final String testNickname = "tester";
    private final String testRole = "ROLE_USER";
    private final String dummyAccessToken = "newAccessToken";
    private final String dummyRefreshToken = "newRefreshToken";
    private final String expiredAccessToken = "expiredAccessToken";
    private final String provider = "local";

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .loginId(testLoginId)
                .email(testEmail)
                .nickname(testNickname)
                .userRole(testRole)
                .build();
    }

    @Test
    void postLoginActions_success() {
        // Given
        when(authentication.getName()).thenReturn(testLoginId);
        GrantedAuthority authority = new SimpleGrantedAuthority(testRole);
        doReturn(Collections.singletonList(authority)).when(authentication).getAuthorities();

        when(tokenProvider.createAccessToken(testLoginId, testRole)).thenReturn(dummyAccessToken);
        when(tokenProvider.createRefreshToken(testLoginId, "local")).thenReturn(dummyRefreshToken);

        // When
        LoginResponse loginResponse = tokenService.postLoginActions(authentication, httpServletResponse);

        // Then
        assertNotNull(loginResponse);
        assertEquals(dummyAccessToken, loginResponse.accessToken());
        assertEquals(testLoginId, loginResponse.user().nickname());
        assertNull(loginResponse.user().email());

        verify(redisService).saveRToken(testLoginId, "local", dummyRefreshToken);
    }

    @Test
    void refreshToken_success() throws IOException {
        // Given
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.getUserIdFromToken(expiredAccessToken)).thenReturn(testLoginId);
        when(redisService.getRToken(testLoginId, provider)).thenReturn(dummyRefreshToken);
        when(tokenProvider.validateRefreshToken(dummyRefreshToken)).thenReturn(true);
        when(tokenProvider.refreshToken(expiredAccessToken, provider)).thenReturn(dummyAccessToken);

        // When
        TokenRefreshResponse response = tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);

        // Then
        assertNotNull(response);
        assertEquals(dummyAccessToken, response.accessToken());
    }

    @Test
    void refreshToken_fail_tokenNotFoundInRedis() throws IOException {
        // Given
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.getUserIdFromToken(expiredAccessToken)).thenReturn(testLoginId);
        when(redisService.getRToken(testLoginId, provider)).thenReturn(null);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);
        });

        assertEquals(ErrorType.REFRESH_TOKEN_NOT_FOUND, exception.getErrorType());
    }

    @Test
    void refreshToken_fail_invalidRefreshToken() throws IOException {
        // Given
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.getUserIdFromToken(expiredAccessToken)).thenReturn(testLoginId);
        when(redisService.getRToken(testLoginId, provider)).thenReturn(dummyRefreshToken);
        when(tokenProvider.validateRefreshToken(dummyRefreshToken)).thenReturn(false);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);
        });

        assertEquals(ErrorType.INVALID_REFRESH_TOKEN, exception.getErrorType());
    }

    @Test
    void refreshToken_fail_userIdNullFromExpiredToken() throws IOException {
        // Given
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.getUserIdFromToken(expiredAccessToken)).thenReturn(null);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);
        });

        assertEquals(ErrorType.INVALID_ACCESS_TOKEN, exception.getErrorType());
        verify(redisService, never()).getRToken(any(), any());
    }
}
