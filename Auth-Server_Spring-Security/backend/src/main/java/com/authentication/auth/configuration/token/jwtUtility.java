package com.authentication.auth.configuration.token;

import com.authentication.auth.DTO.token.TokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class jwtUtility {

    


    public jwtUtility(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.secret-key2}") String secretKey2,
            @Value("${ACCESS_TOKEN_VALIDITY}") Long ACCESS_TOKEN_VALIDITY,
            @Value("${REFRESH_TOKEN_VALIDITY}") Long REFRESH_TOKEN_VALIDITY){
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.key2 = Keys.hmacShaKeyFor(secretKey2.getBytes());
        this.ACCESS_TOKEN_VALIDITY = ACCESS_TOKEN_VALIDITY;
        this.REFRESH_TOKEN_VALIDITY = REFRESH_TOKEN_VALIDITY;
    }


    public TokenDto buildToken(String userId, String nickname, Collection<? extends GrantedAuthority> role) {
        Claims claims = createClaims(userId, nickname, role);

        // 토큰 만료 시간 설정
        Instant now = Instant.now();
        Instant validity = now.plusSeconds(ACCESS_TOKEN_VALIDITY);
        Instant validity2 = now.plusSeconds(REFRESH_TOKEN_VALIDITY);

        // 액세스 토큰 생성
        String accessToken = buildToken(claims, now, validity, key, SecurityConstants.TOKEN_TYPE);
        // 리프레시 토큰 생성
        String refreshToken = buildToken(Jwts.claims(), now, validity2, key2, SecurityConstants.TOKEN_TYPE2);

        return TokenDto.builder()

        return tokenDto.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .build();
    }
/* <<<<<<<<<<  2f21aafa-d159-41a6-892b-76ef4b9b83f8  >>>>>>>>>>> */

    // 클레임 생성 유틸리티 메서드
    private Claims createClaims(String userId, String nickname, Collection<? extends GrantedAuthority> role) {
        Claims claims = Jwts.claims();
        claims.put("userId", userId);
        claims.put("nickname", nickname);
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

    public UsernamePasswordAuthenticationToken getAuthentication(String JWT){
        try {
            Jws<Claims> parsedToken = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(JWT);

            log.info("parsedToken : " + parsedToken);
            users user = new users();

            List<LinkedHashMap<String, String>> roleList =
                    (List<LinkedHashMap<String, String>>) parsedToken.getBody().get("role");

            Role role = Role .valueOf(roleList.get(0).get("authority"));
            log.info("role : {}", role.toString());
            user = user.builder()
                    .userId((String) parsedToken.getBody().get("userId"))
                    .role(role)
                    .build();

            UserDetails userDetails = new principalDetails(user);


            log.info(userDetails.getUsername());
            log.info(userDetails.getAuthorities().toString());

            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        } catch (Exception e){
            log.error("authentication 과정에서 Exception 발생! " + e.getMessage());
            return null;
        }
    }

    public boolean validateJWT(String JWT) {
        try {
            Jws<Claims> parsedToken = Jwts.parserBuilder()
                    .setSigningKey(key)
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
                    .setSigningKey(key2)
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
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(JWT);

            Claims claims = parsedToken.getBody();

            Map<String, Object> claimMap = new HashMap<>();
            claimMap.put("userId", claims.get("userId"));
            claimMap.put("nickname", claims.get("nickname"));
            claimMap.put("role", claims.get("role"));
            return claimMap;
        } catch (ExpiredJwtException e) {
            log.error("| jwt utils | expired Token!");
            Map<String, Object> claimMap = new HashMap<>();
            claimMap.put("userId", e.getClaims().get("userId"));
            claimMap.put("nickname", e.getClaims().get("nickname"));
            claimMap.put("role", e.getClaims().get("role"));
            return claimMap;
        } catch (JwtException e) {
            throw new RuntimeException("token error");
        }
    }


    // 기존 토큰을 기반으로 새로운 토큰을 발급하는 메서드
    public String refreshToken(String expiredToken) {
        Claims claims; Instant now = Instant.now();Instant newExpiry = now.plusSeconds(ACCESS_TOKEN_VALIDITY);
        try {
            claims = extractClaims(expiredToken);
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
        } catch (JwtException e) {
            throw new RuntimeException("token error");
        }
        return buildToken(claims, now, newExpiry, key, SecurityConstants.TOKEN_TYPE);
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
                .setSigningKey(key)
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


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
