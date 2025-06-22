package com.authentication.auth.configuration.token;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.others.constants.SecurityConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtility {

    private final Key key;
    private final Key key2;
    private final Long ACCESS_TOKEN_VALIDITY; // in seconds
    private final Long REFRESH_TOKEN_VALIDITY; // in seconds

    public JwtUtility(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes());
        this.key2 = Keys.hmacShaKeyFor(jwtProperties.secretKey2().getBytes());
        this.ACCESS_TOKEN_VALIDITY = jwtProperties.accessTokenExpirationMinutes() * 60L;
        this.REFRESH_TOKEN_VALIDITY = jwtProperties.refreshTokenExpirationMinutes() * 60L;
    }


    public TokenDto buildToken(String userId, Collection<? extends GrantedAuthority> role) {
        Claims claims = createClaims(userId, role);

        // 토큰 만료 시간 설정
        Instant now = Instant.now();
        Instant validity = now.plusSeconds(this.ACCESS_TOKEN_VALIDITY);
        Instant validity2 = now.plusSeconds(this.REFRESH_TOKEN_VALIDITY);

        // 액세스 토큰 생성
        String accessToken = buildToken(claims, now, validity, this.key, SecurityConstants.TOKEN_TYPE.getValue());
        // 리프레시 토큰 생성
        String refreshToken = buildToken(Jwts.claims(), now, validity2, this.key2, SecurityConstants.REFRESH_TOKEN_TYPE.getValue());

        return new TokenDto(accessToken, refreshToken);
    }
/* <<<<<<<<<<  2f21aafa-d159-41a6-892b-76ef4b9b83f8  >>>>>>>>>>> */

    // 클레임 생성 유틸리티 메서드
    private Claims createClaims(String userId, Collection<? extends GrantedAuthority> role) {
        Claims claims = Jwts.claims();
        claims.put("userId", userId);
        claims.put("role", role);
        return claims;
    }

    // 토큰 생성 유틸리티 메서드
    private String buildToken(Claims claims, Instant issuedAt, Instant expiry, Key signingKey, String type) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiry))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .claim("type", type)
                .compact();
    }

    @SuppressWarnings("unchecked") // Added to suppress unchecked cast warning
    public UsernamePasswordAuthenticationToken getAuthentication(String JWT){
        try {
            Jws<Claims> parsedToken = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(JWT);

            log.info("parsedToken : " + parsedToken);

            List<LinkedHashMap<String, String>> roleList =
                    (List<LinkedHashMap<String, String>>) parsedToken.getBody().get("role");

            List<String> roles = roleList.stream()
                    .map(roleMap -> roleMap.get("authority"))
                    .collect(Collectors.toList());

            String userId = parsedToken.getBody().get("userId").toString();

            User userForPrincipal = User.builder()
                    .nickname(userId)
                    .email("dummy@email.com")
                    .build();

            UserDetails principalDetails = new PrincipalDetails(userForPrincipal);

            Collection<GrantedAuthority> authorities = Collections.singletonList(
                    // Assuming roles list is not empty and we take the first role for SimpleGrantedAuthority
                    // or that userRole field in User entity is meant to be the primary role string.
                    new SimpleGrantedAuthority("ROLE_" + (roles.isEmpty() ? "USER" : roles.get(0)))
            );

            return new UsernamePasswordAuthenticationToken(principalDetails, "", authorities);
        } catch (ExpiredJwtException e) {
            log.error("authentication 과정에서 Exception 발생! " + e.getMessage());
            return null;
        }
    }

    public boolean validateJWT(String JWT) {
        try {
            Jws<Claims> parsedToken = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(JWT);

            log.info("JWT 유효성 검증 통과 - 만료일: {}", parsedToken.getBody().getExpiration());
            return !parsedToken.getBody().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.error("JWT 만료 - {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT - {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 JWT - {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("JWT 예외 발생 - {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT 처리 중 일반 예외 발생 - {}", e.getMessage());
            return false;
        }

    }

    public boolean validateRefreshJWT(String refreshJWT) {
        try{
            Jws<Claims> parsedRefreshToken = Jwts.parserBuilder()
                    .setSigningKey(this.key2)
                    .build()
                    .parseClaimsJws(refreshJWT);
            return !parsedRefreshToken.getBody().getExpiration().before(new Date());
        } catch(ExpiredJwtException e){
            log.error("refresh token expired");
            return false;
        } catch (JwtException e){
            log.error("refresh token tampered");
            return false;
        } catch(NullPointerException e){
            log.error("refresh token is null");
            return false;
        } catch (Exception e){
            log.error("refresh token error");
            return false;
        }
    }



    public Map<String, Object> getClaimsFromAccessToken(String JWT) {
        try {
            Jws<Claims> parsedToken = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(JWT);

            Claims claims = parsedToken.getBody();

            Map<String, Object> claimMap = new HashMap<>();
            claimMap.put("userId", claims.get("userId"));
            claimMap.put("role", claims.get("role"));
            return claimMap;
        } catch (ExpiredJwtException e) {
            log.error("| jwt utils | expired Token!");
            Map<String, Object> claimMap = new HashMap<>();
            claimMap.put("userId", e.getClaims().get("userId"));
            claimMap.put("role", e.getClaims().get("role"));
            return claimMap;
        } catch (JwtException e) {
            throw new RuntimeException("token error");
        }
    }


    // 기존 토큰을 기반으로 새로운 토큰을 발급하는 메서드
    public String refreshToken(String expiredToken) {
        Claims claims; Instant now = Instant.now();Instant newExpiry = now.plusSeconds(this.ACCESS_TOKEN_VALIDITY);
        try {
            claims = extractClaims(expiredToken);
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
        } catch (JwtException e) {
            throw new RuntimeException("token error");
        }
        return buildToken(claims, now, newExpiry, this.key, SecurityConstants.TOKEN_TYPE.getValue());
    }


    public String getUserIdFromToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token is null");
        }

        Claims getClaims = this.extractClaims(token);
        return getClaims.get("userId", String.class);  // userId를 String으로 반환
    }


    // access 토큰에서 Claims 를 추출하는 유틸리티 메서드
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(this.key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String checkSnsCookie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String providerHeader = request.getHeader("provider"); // SNS 정보 제공자 추가하기
        Cookie[] cookies = request.getCookies();
        String RToken = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(providerHeader + "_refreshToken")) {
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

    private void sendResponseStatus(HttpServletResponse response, int status, String message)
            throws IOException{
        response.setStatus(status);
        PrintWriter writer = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        writer.print("{\"message\":\"" + message + "\"}");
        writer.flush();
    }


    @Transactional
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



}
