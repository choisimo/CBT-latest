package com.authentication.auth.controller.auth;

import com.authentication.auth.dto.response.LoginResponse;
import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.service.token.TokenService;
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
    private Authentication authentication; // Mocked Authentication object

    private LoginRequest loginRequest;
    private TokenRefreshRequest tokenRefreshRequest;
    private final String testUserId = "testUser";
    private final String testPassword = "password";
    private final String dummyAccessToken = "dummyAccessToken";
    private final String dummyExpiredToken = "expiredToken";
    private final String dummyProvider = "local";

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest(testUserId, testPassword);
        tokenRefreshRequest = new TokenRefreshRequest(dummyExpiredToken, dummyProvider);
    }

    @Test
    void login_success() {
        LoginResponse mockLoginResponse = new LoginResponse(dummyAccessToken);
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(testUserId, testPassword);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication); // Return the mocked Authentication
        when(tokenService.postLoginActions(authentication, httpServletResponse)).thenReturn(mockLoginResponse);

        ResponseEntity<LoginResponse> responseEntity = tokenController.login(loginRequest, httpServletResponse);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockLoginResponse, responseEntity.getBody());
        verify(authenticationManager).authenticate(argThat(token ->
                token.getName().equals(testUserId) && token.getCredentials().equals(testPassword)
        ));
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
        ResponseEntity<Object> mockServiceResponse = ResponseEntity.ok().body("newAccessToken");
        when(tokenService.refreshToken(httpServletRequest, httpServletResponse, tokenRefreshRequest))
                .thenReturn(mockServiceResponse);

        ResponseEntity<?> responseEntity = tokenController.refreshToken(httpServletRequest, httpServletResponse, tokenRefreshRequest);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("newAccessToken", responseEntity.getBody());
        verify(tokenService).refreshToken(httpServletRequest, httpServletResponse, tokenRefreshRequest);
    }

    @Test
    void refreshToken_invalidPayload_nullRequest() throws IOException {
        ResponseEntity<?> responseEntity = tokenController.refreshToken(httpServletRequest, httpServletResponse, null);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().toString().contains("Invalid refresh token request payload"));
        verify(tokenService, never()).refreshToken(any(), any(), any());
    }

    @Test
    void refreshToken_invalidPayload_nullExpiredToken() throws IOException {
        TokenRefreshRequest invalidRequest = new TokenRefreshRequest(null, dummyProvider);
        ResponseEntity<?> responseEntity = tokenController.refreshToken(httpServletRequest, httpServletResponse, invalidRequest);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().toString().contains("Invalid refresh token request payload"));
        verify(tokenService, never()).refreshToken(any(), any(), any());
    }

    @Test
    void refreshToken_invalidPayload_nullProvider() throws IOException {
        TokenRefreshRequest invalidRequest = new TokenRefreshRequest(dummyExpiredToken, null);
        ResponseEntity<?> responseEntity = tokenController.refreshToken(httpServletRequest, httpServletResponse, invalidRequest);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().toString().contains("Invalid refresh token request payload"));
        verify(tokenService, never()).refreshToken(any(), any(), any());
    }
}
