package com.authentication.auth.controller.auth;

import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.dto.token.TokenRefreshResponse;
import com.authentication.auth.service.token.TokenService;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private TokenController tokenController;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private Authentication authentication;

    private LoginRequest loginRequest;
    private TokenRefreshRequest tokenRefreshRequest;
    private final String testUserId = "testUser";
    private final String testEmail = "test@example.com";
    private final String testPassword = "password";
    private final String dummyAccessToken = "dummyAccessToken";
    private final String dummyExpiredToken = "expiredToken";
    private final String dummyProvider = "local";

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest(testUserId, null, testPassword);
        tokenRefreshRequest = new TokenRefreshRequest(dummyExpiredToken, dummyProvider);
    }

    @Test
    void login_success() {
        LoginResponse.UserInfo mockUserInfo = new LoginResponse.UserInfo(1L, testUserId, testEmail);
        LoginResponse mockLoginResponse = new LoginResponse(dummyAccessToken, mockUserInfo);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenService.postLoginActions(authentication, httpServletResponse)).thenReturn(mockLoginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> responseEntity = tokenController.login(loginRequest, httpServletResponse);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(mockLoginResponse, responseEntity.getBody().data());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).postLoginActions(authentication, httpServletResponse);
    }

    @Test
    void login_failure_badCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> {
            tokenController.login(loginRequest, httpServletResponse);
        });

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, never()).postLoginActions(any(Authentication.class), any(HttpServletResponse.class));
    }

    @Test
    void refreshToken_success() throws IOException {
        TokenRefreshResponse mockServiceResponse = new TokenRefreshResponse("newAccessToken");
        when(tokenService.refreshToken(any(), any(), any())).thenReturn(mockServiceResponse);

        ResponseEntity<ApiResponse<TokenRefreshResponse>> responseEntity = tokenController.refreshToken(httpServletRequest, httpServletResponse, tokenRefreshRequest);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("newAccessToken", responseEntity.getBody().data().accessToken());
        verify(tokenService).refreshToken(httpServletRequest, httpServletResponse, tokenRefreshRequest);
    }

    @Test
    void refreshToken_invalidPayload_nullRequest() throws IOException {
        CustomException exception = assertThrows(CustomException.class, () -> {
            tokenController.refreshToken(httpServletRequest, httpServletResponse, null);
        });

        assertEquals(ErrorType.INVALID_REQUEST_BODY, exception.getErrorType());
        assertEquals("유효하지 않은 토큰 갱신 요청입니다.", exception.getMessage());
        verify(tokenService, never()).refreshToken(any(), any(), any());
    }

    @Test
    void refreshToken_invalidPayload_nullExpiredToken() throws IOException {
        TokenRefreshRequest invalidRequest = new TokenRefreshRequest(null, dummyProvider);

        CustomException exception = assertThrows(CustomException.class, () -> {
            tokenController.refreshToken(httpServletRequest, httpServletResponse, invalidRequest);
        });

        assertEquals(ErrorType.INVALID_REQUEST_BODY, exception.getErrorType());
        assertEquals("유효하지 않은 토큰 갱신 요청입니다.", exception.getMessage());
        verify(tokenService, never()).refreshToken(any(), any(), any());
    }

    @Test
    void refreshToken_invalidPayload_nullProvider() throws IOException {
        TokenRefreshRequest invalidRequest = new TokenRefreshRequest(dummyExpiredToken, null);

        CustomException exception = assertThrows(CustomException.class, () -> {
            tokenController.refreshToken(httpServletRequest, httpServletResponse, invalidRequest);
        });

        assertEquals(ErrorType.INVALID_REQUEST_BODY, exception.getErrorType());
        assertEquals("유효하지 않은 토큰 갱신 요청입니다.", exception.getMessage());
        verify(tokenService, never()).refreshToken(any(), any(), any());
    }
}
