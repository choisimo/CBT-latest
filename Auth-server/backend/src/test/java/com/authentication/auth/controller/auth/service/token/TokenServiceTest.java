package test.java.com.authentication.auth.controller.auth.service.token;
import com.authentication.auth.dto.response.LoginResponse;
import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.service.redis.RedisService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

    private final String testUserId = "testUser";
    private final String testRole = "ROLE_USER";
    private final String dummyAccessToken = "newAccessToken";
    private final String dummyRefreshToken = "newRefreshToken";
    private final String expiredAccessToken = "expiredAccessToken";
    private final String provider = "local";
    // Using reflection or a different approach for private static final is too complex for this context.
    // We'll use the actual value if TokenService.REFRESH_TOKEN_TTL_SECONDS is accessible,
    // otherwise, define a matching constant here for test clarity.
    // For now, assume it's not directly accessible and define it.
    private static final int REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60;


    @BeforeEach
    void setUp() {
        // Common setup
    }

    @Test
    void postLoginActions_success() {
        when(authentication.getName()).thenReturn(testUserId);
        GrantedAuthority authority = new SimpleGrantedAuthority(testRole);
        when(authentication.getAuthorities()).thenReturn(Collections.singletonList(authority));

        when(tokenProvider.createAccessToken(testUserId, testRole)).thenReturn(dummyAccessToken);
        when(tokenProvider.createRefreshToken(testUserId, "local")).thenReturn(dummyRefreshToken);

        // Mocking the static method setHttpOnlyCookie from TokenProvider
        try (MockedStatic<TokenProvider> mockedTokenProviderUtil = Mockito.mockStatic(TokenProvider.class)) {
            mockedTokenProviderUtil.when(() -> TokenProvider.setHttpOnlyCookie(any(HttpServletResponse.class), eq("refreshToken"), eq(dummyRefreshToken), eq(REFRESH_TOKEN_TTL_SECONDS)))
                    .doesNothing(); // Static method is void

            LoginResponse loginResponse = tokenService.postLoginActions(authentication, httpServletResponse);

            assertNotNull(loginResponse);
            assertEquals(dummyAccessToken, loginResponse.getAccessToken());

            verify(redisService).saveRefreshToken(testUserId, "local", dummyRefreshToken, REFRESH_TOKEN_TTL_SECONDS);
            mockedTokenProviderUtil.verify(() -> TokenProvider.setHttpOnlyCookie(httpServletResponse, "refreshToken", dummyRefreshToken, REFRESH_TOKEN_TTL_SECONDS));
        }
    }
    
    @Test
    void postLoginActions_success_noAuthorities() {
        when(authentication.getName()).thenReturn(testUserId);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList()); // No authorities

        when(tokenProvider.createAccessToken(testUserId, "ROLE_USER")).thenReturn(dummyAccessToken); // Default role
        when(tokenProvider.createRefreshToken(testUserId, "local")).thenReturn(dummyRefreshToken);

        try (MockedStatic<TokenProvider> mockedTokenProviderUtil = Mockito.mockStatic(TokenProvider.class)) {
            mockedTokenProviderUtil.when(() -> TokenProvider.setHttpOnlyCookie(any(HttpServletResponse.class), eq("refreshToken"), eq(dummyRefreshToken), eq(REFRESH_TOKEN_TTL_SECONDS)))
                    .doesNothing();

            LoginResponse loginResponse = tokenService.postLoginActions(authentication, httpServletResponse);

            assertNotNull(loginResponse);
            assertEquals(dummyAccessToken, loginResponse.getAccessToken());
            verify(tokenProvider).createAccessToken(testUserId, "ROLE_USER"); // Verify default role was used
            verify(redisService).saveRefreshToken(testUserId, "local", dummyRefreshToken, REFRESH_TOKEN_TTL_SECONDS);
            mockedTokenProviderUtil.verify(() -> TokenProvider.setHttpOnlyCookie(httpServletResponse, "refreshToken", dummyRefreshToken, REFRESH_TOKEN_TTL_SECONDS));
        }
    }


    @Test
    void refreshToken_success() throws IOException {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.checkCookie(httpServletRequest, httpServletResponse, provider)).thenReturn(dummyRefreshToken);
        when(tokenProvider.validateRefreshToken(dummyRefreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(expiredAccessToken)).thenReturn(testUserId);
        when(redisService.isRTokenExist(testUserId, provider, dummyRefreshToken)).thenReturn(true);
        when(tokenProvider.refreshToken(expiredAccessToken, provider)).thenReturn(dummyAccessToken);

        ResponseEntity<?> responseEntity = tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(dummyAccessToken, responseEntity.getBody());
    }

    @Test
    void refreshToken_fail_cookieNull() throws IOException {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.checkCookie(httpServletRequest, httpServletResponse, provider)).thenReturn(null);

        ResponseEntity<?> responseEntity = tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().toString().contains("Refresh token not found in cookie"));
    }

    @Test
    void refreshToken_fail_invalidRefreshToken() throws IOException {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.checkCookie(httpServletRequest, httpServletResponse, provider)).thenReturn(dummyRefreshToken);
        when(tokenProvider.validateRefreshToken(dummyRefreshToken)).thenReturn(false);

        ResponseEntity<?> responseEntity = tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().toString().contains("Invalid or expired refresh token"));
    }
    
    @Test
    void refreshToken_fail_userIdNullFromExpiredToken() throws IOException {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.checkCookie(httpServletRequest, httpServletResponse, provider)).thenReturn(dummyRefreshToken);
        when(tokenProvider.validateRefreshToken(dummyRefreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(expiredAccessToken)).thenReturn(null); // Simulate failure to get user ID

        ResponseEntity<?> responseEntity = tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().toString().contains("Could not extract user ID from expired token"));
    }

    @Test
    void refreshToken_fail_notInRedis() throws IOException {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(expiredAccessToken, provider);
        when(tokenProvider.checkCookie(httpServletRequest, httpServletResponse, provider)).thenReturn(dummyRefreshToken);
        when(tokenProvider.validateRefreshToken(dummyRefreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(expiredAccessToken)).thenReturn(testUserId);
        when(redisService.isRTokenExist(testUserId, provider, dummyRefreshToken)).thenReturn(false);

        ResponseEntity<?> responseEntity = tokenService.refreshToken(httpServletRequest, httpServletResponse, refreshRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().toString().contains("Refresh token not found in Redis or mismatch"));
    }
}
