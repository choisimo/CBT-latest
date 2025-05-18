package com.authentication.auth.configuration.token;

import com.authentication.auth.constants.ErrorType;
import com.authentication.auth.constants.SecurityConstants;
import com.authentication.auth.dto.token.TokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtility {

    private final String domain;
    private final String cookieDomain;
    private final Key accessTokenKey;
    private final Key refreshTokenKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtUtility(
            @Value("${site.domain}") String domain,
            @Value("${server.cookie.domain}") String cookieDomain,
            @Value("${jwt.secret-key}") String accessTokenSecret,
            @Value("${jwt.secret-key2}") String refreshTokenSecret,
            @Value("${ACCESS_TOKEN_VALIDITY}") Long accessTokenValidity,
            @Value("${REFRESH_TOKEN_VALIDITY}") Long refreshTokenValidity) {
        this.domain = domain;
        this.cookieDomain = cookieDomain;
        this.accessTokenKey = Keys.hmacShaKeyFor(accessTokenSecret.getBytes());
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes());
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * 토큰 DTO 생성
     * @param username 사용자 이름
     * @param authorities 권한 목록
     * @return 토큰 DTO
     */
    public TokenDto createTokenDto(String username, Collection<? extends GrantedAuthority> authorities) {
        String accessToken = generateAccessToken(username, authorities);
        String refreshToken = generateRefreshToken(username);
        
        return TokenDto.of(accessToken, refreshToken, accessTokenValidity);
    }

    /**
     * 액세스 토큰 생성
     * @param username 사용자 이름
     * @param authorities 권한 목록
     * @return 생성된 액세스 토큰
     */
    public String generateAccessToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValidity);
        
        Claims claims = Jwts.claims();
        claims.setSubject(username);
        claims.put("authorities", authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(accessTokenKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 리프레시 토큰 생성
     * @param username 사용자 이름
     * @return 생성된 리프레시 토큰
     */
    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenValidity);
        
        Claims claims = Jwts.claims();
        claims.setSubject(username);
        claims.setId(UUID.randomUUID().toString());
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(refreshTokenKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 토큰 유효성 검증
     * @param token 검증할 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(accessTokenKey)
                    .build()
                    .parseClaimsJws(token);
            
            return !claims.getBody().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.error("JWT 만료: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("JWT 예외: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT 처리 중 일반 예외: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 리프레시 토큰 유효성 검증
     * @param refreshToken 검증할 리프레시 토큰
     * @return 유효 여부
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(refreshTokenKey)
                    .build()
                    .parseClaimsJws(refreshToken);
            
            return !claims.getBody().getExpiration().before(new Date());
        } catch (JwtException | NullPointerException e) {
            log.error("리프레시 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 사용자 이름 추출
     * @param token JWT 토큰
     * @return 사용자 이름
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(accessTokenKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getSubject();
    }

    /**
     * 토큰에서 권한 목록 추출
     * @param token JWT 토큰
     * @return 권한 목록
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(accessTokenKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        List<String> authorities = claims.get("authorities", List.class);
        return authorities != null ? new HashSet<>(authorities) : Collections.emptySet();
    }

    /**
     * 토큰에서 클레임 추출
     * @param token JWT 토큰
     * @return 클레임
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessTokenKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 인증 객체 생성
     * @param token JWT 토큰
     * @return 인증 객체
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        
        @SuppressWarnings("unchecked")
        List<String> authorities = claims.get("authorities", List.class);
        
        List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        
        UserDetails principal = new User(claims.getSubject(), "", grantedAuthorities);
        
        return new UsernamePasswordAuthenticationToken(principal, token, grantedAuthorities);
    }

    /**
     * 쿠키에서 리프레시 토큰 확인
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param provider 제공자
     * @return 리프레시 토큰
     */
    public String checkCookie(HttpServletRequest request, HttpServletResponse response, String provider) throws IOException {
        Cookie[] cookies = request.getCookies();
        String refreshToken = null;

        if (cookies == null) {
            sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED);
            return null;
        }

        String cookieName = SecurityConstants.DEFAULT_PROVIDER.getValue().equals(provider) 
                ? SecurityConstants.COOKIE_REFRESH_TOKEN.getValue()
                : provider + "_" + SecurityConstants.COOKIE_REFRESH_TOKEN.getValue();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null) {
            sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED);
        }

        return refreshToken;
    }

    /**
     * SNS 쿠키 확인
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 리프레시 토큰
     */
    public String checkSnsCookie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String providerHeader = request.getHeader("provider");
        Cookie[] cookies = request.getCookies();
        String refreshToken = null;
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(providerHeader + "_" + SecurityConstants.COOKIE_REFRESH_TOKEN.getValue())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        
        if (refreshToken == null) {
            sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED);
        }
        
        return refreshToken;
    }

    /**
     * 오류 응답 전송
     * @param response HTTP 응답
     * @param errorType 오류 유형
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType)
            throws IOException {
        response.setStatus(errorType.getStatusCode());
        PrintWriter writer = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        writer.print("{\"error\":\"" + errorType.getTitle() + "\",\"message\":\"" + errorType.getMessage() + "\"}");
        writer.flush();
    }

    /**
     * 액세스 토큰 만료 시간 반환
     * @return 만료 시간 (초)
     */
    public long getAccessTokenExpiration() {
        return accessTokenValidity * 1000; // 밀리초로 변환
    }

    /**
     * 리프레시 토큰 만료 시간 반환
     * @return 만료 시간 (초)
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenValidity * 1000; // 밀리초로 변환
    }
}
