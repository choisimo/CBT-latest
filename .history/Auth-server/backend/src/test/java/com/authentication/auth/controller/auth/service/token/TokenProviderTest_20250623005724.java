package com.authentication.auth.controller.auth.service.token;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.service.token.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    @Mock
    private JwtUtility jwtUtility;

    @InjectMocks
    private TokenProvider tokenProvider;

    private final String testUserId = "testUser";
    private final String testRole = "ROLE_USER";
    private final String testProvider = "local";
    private final String dummyToken = "dummyTokenValue";
    private final String dummyAccessToken = "dummyAccessToken";
    private final String dummyRefreshToken = "dummyRefreshToken";

    @BeforeEach
    void setUp() {
        // Initialization if needed, though @InjectMocks handles TokenProvider instance
    }

    @Test
    void createAccessToken() {
        TokenDto mockTokenDto = new TokenDto(dummyAccessToken, dummyRefreshToken);
        when(jwtUtility.buildToken(eq(testUserId), anyList())).thenReturn(mockTokenDto);

        String accessToken = tokenProvider.createAccessToken(testUserId, testRole);

        assertEquals(dummyAccessToken, accessToken);
        verify(jwtUtility).buildToken(eq(testUserId), argThat(list ->
                list.size() == 1 && ((SimpleGrantedAuthority)list.iterator().next()).getAuthority().equals(testRole)
        ));
    }

    @Test
    void createRefreshToken() {
        TokenDto mockTokenDto = new TokenDto(dummyAccessToken, dummyRefreshToken);
        // For refresh token, role list is empty in current TokenProvider impl
        when(jwtUtility.buildToken(eq(testUserId), eq(Collections.emptyList()))).thenReturn(mockTokenDto);

        String refreshToken = tokenProvider.createRefreshToken(testUserId, testProvider);

        assertEquals(dummyRefreshToken, refreshToken);
        verify(jwtUtility).buildToken(eq(testUserId), eq(Collections.emptyList()));
    }

    @Test
    void validateToken_valid() {
        when(jwtUtility.validateJWT(dummyToken)).thenReturn(true);
        assertTrue(tokenProvider.validateToken(dummyToken));
        verify(jwtUtility).validateJWT(dummyToken);
    }

    @Test
    void validateToken_invalid() {
        when(jwtUtility.validateJWT(dummyToken)).thenReturn(false);
        assertFalse(tokenProvider.validateToken(dummyToken));
        verify(jwtUtility).validateJWT(dummyToken);
    }

    @Test
    void getUserIdFromToken_success() {
        when(jwtUtility.getUserIdFromToken(dummyToken)).thenReturn(testUserId);
        assertEquals(testUserId, tokenProvider.getUserIdFromToken(dummyToken));
        verify(jwtUtility).getUserIdFromToken(dummyToken);
    }

    @Test
    void getUserIdFromToken_failure() {
        when(jwtUtility.getUserIdFromToken(dummyToken)).thenReturn(null);
        assertNull(tokenProvider.getUserIdFromToken(dummyToken));
        verify(jwtUtility).getUserIdFromToken(dummyToken);
    }
    
    @Test
    void getExpiration_success() {
        Claims mockClaims = new DefaultClaims();
        long futureTime = System.currentTimeMillis() + 100000;
        mockClaims.setExpiration(new Date(futureTime));
        when(jwtUtility.extractClaims(dummyToken)).thenReturn(mockClaims);

        Long expiration = tokenProvider.getExpiration(dummyToken);

        assertNotNull(expiration);
        assertEquals(futureTime, expiration);
        verify(jwtUtility).extractClaims(dummyToken);
    }

    @Test
    void getExpiration_tokenInvalid() {
        when(jwtUtility.extractClaims(dummyToken)).thenThrow(new RuntimeException("Invalid token"));
        assertNull(tokenProvider.getExpiration(dummyToken));
        verify(jwtUtility).extractClaims(dummyToken);
    }
    
    @Test
    void refreshToken_success() {
        String expiredToken = "expiredTestToken";
        String newAccessToken = "newAccessToken";
        when(jwtUtility.refreshToken(expiredToken)).thenReturn(newAccessToken);

        String result = tokenProvider.refreshToken(expiredToken, testProvider);

        assertEquals(newAccessToken, result);
        verify(jwtUtility).refreshToken(expiredToken);
    }

    @Test
    void validateRefreshToken_valid() {
        when(jwtUtility.validateRefreshJWT(dummyRefreshToken)).thenReturn(true);
        assertTrue(tokenProvider.validateRefreshToken(dummyRefreshToken));
        verify(jwtUtility).validateRefreshJWT(dummyRefreshToken);
    }

    @Test
    void validateRefreshToken_invalid() {
        when(jwtUtility.validateRefreshJWT(dummyRefreshToken)).thenReturn(false);
        assertFalse(tokenProvider.validateRefreshToken(dummyRefreshToken));
        verify(jwtUtility).validateRefreshJWT(dummyRefreshToken);
    }

    @Test
    void resolveToken_withBearerToken() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + dummyToken);

        String resolvedToken = tokenProvider.resolveToken(mockRequest);
        assertEquals(dummyToken, resolvedToken);
    }

    @Test
    void resolveToken_withoutBearerPrefix() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("Authorization")).thenReturn(dummyToken);

        String resolvedToken = tokenProvider.resolveToken(mockRequest);
        assertNull(resolvedToken); // Should be null as "Bearer " prefix is missing
    }

    @Test
    void resolveToken_noAuthHeader() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("Authorization")).thenReturn(null);

        String resolvedToken = tokenProvider.resolveToken(mockRequest);
        assertNull(resolvedToken);
    }
    
    @Test
    void setHttpOnlyCookie() {
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String cookieName = "myCookie";
        String cookieValue = "myValue";
        int maxAge = 3600;

        TokenProvider.setHttpOnlyCookie(mockResponse, cookieName, cookieValue, maxAge);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(mockResponse).addCookie(cookieCaptor.capture());

        Cookie capturedCookie = cookieCaptor.getValue();
        assertEquals(cookieName, capturedCookie.getName());
        assertEquals(cookieValue, capturedCookie.getValue());
        assertTrue(capturedCookie.isHttpOnly());
        assertEquals("/", capturedCookie.getPath());
        assertEquals(maxAge, capturedCookie.getMaxAge());
    }

    @Test
    void checkCookie_cookieExists_serverProvider() throws IOException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        Cookie[] cookies = {new Cookie("refreshToken", dummyRefreshToken)};
        when(mockRequest.getCookies()).thenReturn(cookies);

        String token = tokenProvider.checkCookie(mockRequest, mockResponse, "server");
        assertEquals(dummyRefreshToken, token);
        verify(mockResponse, never()).setStatus(anyInt()); // No error status should be set
    }

    @Test
    void checkCookie_cookieExists_otherProvider() throws IOException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String provider = "google";
        Cookie[] cookies = {new Cookie(provider + "_refreshToken", dummyRefreshToken)};
        when(mockRequest.getCookies()).thenReturn(cookies);

        String token = tokenProvider.checkCookie(mockRequest, mockResponse, provider);
        assertEquals(dummyRefreshToken, token);
        verify(mockResponse, never()).setStatus(anyInt());
    }
    
    @Test
    void checkCookie_noCookies() throws IOException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getCookies()).thenReturn(null);
        when(mockResponse.getWriter()).thenReturn(printWriter);


        String token = tokenProvider.checkCookie(mockRequest, mockResponse, "server");

        assertNull(token);
        verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
        assertTrue(stringWriter.toString().contains("there's no cookies"));
    }

    @Test
    void checkCookie_targetCookieMissing() throws IOException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        Cookie[] cookies = {new Cookie("otherCookie", "otherValue")};
        when(mockRequest.getCookies()).thenReturn(cookies);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        String token = tokenProvider.checkCookie(mockRequest, mockResponse, "server");

        assertNull(token);
        verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(stringWriter.toString().contains("there's no refreshToken"));
    }
}
