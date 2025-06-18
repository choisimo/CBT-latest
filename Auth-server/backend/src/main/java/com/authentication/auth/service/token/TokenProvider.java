package com.authentication.auth.service.token;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.token.TokenDto;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;

@Component
public class TokenProvider {

    private final JwtUtility jwtUtility;

    @Autowired
    public TokenProvider(JwtUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    public String createAccessToken(String userId, String role) {
        TokenDto tokenDto = jwtUtility.buildToken(userId, Collections.singletonList(new SimpleGrantedAuthority(role)));
        return tokenDto.accessToken();
    }

    public String createRefreshToken(String userId, String provider) {
        // provider parameter is not directly used in jwtUtility.buildToken for refresh token generation in the current JwtUtility structure.
        // JwtUtility.buildToken creates both access and refresh tokens based on userId and role.
        // If provider specific logic is needed for refresh token, JwtUtility might need adjustments.
        TokenDto tokenDto = jwtUtility.buildToken(userId, Collections.emptyList()); // Role might not be relevant for refresh token or use a default/generic one
        return tokenDto.refreshToken();
    }

    public boolean validateToken(String token) {
        return jwtUtility.validateJWT(token);
    }

    public String getUserIdFromToken(String token) {
        return jwtUtility.getUserIdFromToken(token);
    }

    public String getProviderFromToken(String token) {
        // JWTs created by JwtUtility do not typically store a 'provider' claim.
        // This method might need a different approach if provider information is crucial.
        // For now, returning a default or null, as JwtUtility doesn't extract this.
        // Claims claims = jwtUtility.extractClaims(token);
        // return claims.get("provider", String.class); // Example if provider claim existed
        return "server"; // Placeholder, consistent with original, assuming server-generated tokens
    }

    public Long getExpiration(String token) {
        try {
            Claims claims = jwtUtility.extractClaims(token);
            return claims.getExpiration().getTime();
        } catch (Exception e) {
            // Handle cases where token is invalid or expiration cannot be extracted
            return null;
        }
    }

    public boolean validateRefreshToken(String refreshToken) {
        return jwtUtility.validateRefreshJWT(refreshToken);
    }

    public String refreshToken(String expiredAccessToken, String provider) {
        // The 'provider' parameter is not directly used by jwtUtility.refreshToken.
        // jwtUtility.refreshToken reuses claims from the expired access token.
        // If provider-specific logic is needed here (e.g., different token validity),
        // JwtUtility or the logic here would need further adaptation.
        return jwtUtility.refreshToken(expiredAccessToken);
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // Copied from JwtUtility
    public String checkCookie(HttpServletRequest request, HttpServletResponse response, String provider) throws IOException {
        Cookie[] cookies = request.getCookies();
        String RToken = null;

        if (cookies == null) {
            sendResponseStatus(response, HttpServletResponse.SC_UNAUTHORIZED, "there's no cookies");
            return null;
        }

        if (!"server".equals(provider)) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(provider + "_refreshToken")) {
                    RToken = cookie.getValue();
                    break;
                }
            }
        } else {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    RToken = cookie.getValue();
                    break;
                }
            }
        }

        if (RToken == null) {
            sendResponseStatus(response, HttpServletResponse.SC_UNAUTHORIZED, "there's no refreshToken");
        }

        return RToken;
    }

    // Copied from JwtUtility
    private void sendResponseStatus(HttpServletResponse response, int status, String message)
            throws IOException{
        response.setStatus(status);
        PrintWriter writer = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        writer.print("{\"message\":\"" + message + "\"}");
        writer.flush();
    }

    public static void setHttpOnlyCookie(HttpServletResponse response, String cookieName, String cookieValue, int maxAgeSeconds) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setHttpOnly(true);
        cookie.setPath("/"); // Set cookie path to be accessible across the application
        cookie.setMaxAge(maxAgeSeconds);
        // cookie.setSecure(true); // Recommended to be true in production (requires HTTPS)
        response.addCookie(cookie);
    }
}
