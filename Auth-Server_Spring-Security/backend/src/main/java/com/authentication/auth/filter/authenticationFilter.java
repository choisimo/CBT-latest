package com.career_block.auth.filter;

import com.authentication.auth.DTO.token.principalDetails;
import com.authentication.auth.DTO.token.tokenDto;
import com.authentication.auth.DTO.users.loginRequest;
import com.authentication.auth.configuration.token.jwtUtility;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.redis.redisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
package com.authentication.auth.filter;

import com.authentication.auth.domain.User;
import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.utility.JwtUtility;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: JWT 인증 필터
 * @Details: 사용자 로그인 요청을 처리하고, 인증 성공 시 JWT 토큰을 발급하는 필터
 * @Usage: Spring Security 필터 체인에 등록하여 사용
 */
@Slf4j
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter implements PluggableFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtility jwtUtility;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final String domain;
    private final String cookieDomain;

    /**
     * 인증 필터 생성자
     * @param authenticationManager Spring Security 인증 관리자
     * @param jwtUtility JWT 토큰 유틸리티
     * @param objectMapper JSON 변환용 객체 매퍼
     * @param redisService Redis 서비스 (토큰 저장용)
     * @param domain 애플리케이션 도메인
     * @param cookieDomain 쿠키에 사용할 도메인
     */
    public AuthenticationFilter(AuthenticationManager authenticationManager, JwtUtility jwtUtility, ObjectMapper objectMapper, RedisService redisService, String domain, String cookieDomain) {
        this.authenticationManager = authenticationManager;
        this.jwtUtility = jwtUtility;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.domain = domain;
        this.cookieDomain = cookieDomain;
        // 로그인 URL 설정 - POST /api/auth/login으로 변경
        setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/api/auth/login", "POST"));
    }

    /**
     * 사용자 인증 시도 메서드
     * 요청에서 사용자 자격 증명을 추출하고 인증을 시도합니다.
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 인증 객체
     * @throws AuthenticationException 인증 예외
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        log.info("인증 시도: {}", request.getRequestURI());
        
        try {
            // 요청 바디에서 사용자 자격 증명 추출
            Map<String, String> credentials = objectMapper.readValue(request.getInputStream(), Map.class);
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            if (username == null || password == null) {
                throw new BadCredentialsException("사용자 이름 또는 비밀번호가 누락되었습니다.");
            }
            
            log.debug("사용자 인증 시도: {}", username);
            
            // 인증 토큰 생성 및 인증 시도
            UsernamePasswordAuthenticationToken authenticationToken = 
                new UsernamePasswordAuthenticationToken(username, password);
                
            return authenticationManager.authenticate(authenticationToken);
        } catch (IOException e) {
            log.error("인증 요청 처리 중 오류 발생: {}", e.getMessage());
            throw new BadCredentialsException("인증 요청을 처리할 수 없습니다.");
        }
    }

    /**
     * 인증 성공 처리 메서드
     * 인증 성공 시 JWT 토큰을 생성하고 클라이언트에게 제공합니다.
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param chain 필터 체인
     * @param authResult 인증 결과
     * @throws IOException IO 예외
     * @throws ServletException 서블릿 예외
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult) 
                                           throws IOException, ServletException {
        log.info("인증 성공 처리");
        
        UserDetails userDetails = (UserDetails) authResult.getPrincipal();
        String username = userDetails.getUsername();
        
        // 액세스 토큰 생성
        String accessToken = jwtUtility.generateAccessToken(username, userDetails.getAuthorities());
        
        // 리프레시 토큰 생성 및 Redis에 저장
        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = jwtUtility.generateRefreshToken(username, refreshTokenId);
        
        // 사용자 ID 정보 추출
        User user = (User) userDetails;
        String userId = user.getId().toString();
        
        // Redis에 리프레시 토큰 저장 (사용자 ID와 함께)
        String redisKey = "JWT_RToken_" + userId + "_" + refreshTokenId;
        redisService.setValueWithExpiration(redisKey, refreshToken, jwtUtility.getRefreshTokenExpiration());
        
        // 액세스 토큰과 리프레시 토큰을 쿠키에 설정
        Cookie accessTokenCookie = createCookie("access_token", accessToken, jwtUtility.getAccessTokenExpiration() / 1000);
        Cookie refreshTokenCookie = createCookie("refresh_token", refreshToken, jwtUtility.getRefreshTokenExpiration() / 1000);
        
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
        
        // 응답 데이터 구성
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("status", "success");
        responseData.put("message", "로그인 성공");
        responseData.put("timestamp", LocalDateTime.now().toString());
        
        // JSON 응답 전송
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        objectMapper.writeValue(response.getWriter(), responseData);
        
        log.debug("사용자 {} 인증 완료, 토큰 발급 성공", username);
    }

    /**
     * 쿠키 생성 헬퍼 메서드
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 쿠키 만료 시간 (초)
     * @return 생성된 쿠키
     */
    private Cookie createCookie(String name, String value, long maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS에서만 전송
        cookie.setPath("/");
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge((int) maxAge);
        return cookie;
    }

    /**
     * 인증 실패 처리 메서드
     * 인증 실패 시 적절한 오류 메시지를 클라이언트에게 제공합니다.
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param failed 인증 예외
     * @throws IOException IO 예외
     * @throws ServletException 서블릿 예외
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                             AuthenticationException failed) 
                                             throws IOException, ServletException {
        log.warn("인증 실패: {}", failed.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "로그인 실패: 사용자 이름 또는 비밀번호가 올바르지 않습니다.");
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    /**
     * PluggableFilter 인터페이스 구현
     * 필터 설정 메서드
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilter(this);
    }

    /**
     * PluggableFilter 인터페이스 구현
     * 필터 순서 반환 메서드
     */
    @Override
    public int getOrder() {
        return 2; // 인증 필터는 우선순위가 비교적 높아야 함
    }

    /**
     * PluggableFilter 인터페이스 구현
     * 이 필터 이전에 적용될 필터 클래스 반환
     */
    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return UsernamePasswordAuthenticationFilter.class;
    }

    /**
     * PluggableFilter 인터페이스 구현
     * 이 필터 이후에 적용될 필터 클래스 반환
     */
    @Override
    public Class<? extends Filter> getAfterFilter() {
        return null; // 현재 필터 이후에 특정 필터가 없음
    }

    /**
     * PluggableFilter 인터페이스 구현
     * 필터 체인 로직 실행 메서드
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        // 기본 필터 로직은 UsernamePasswordAuthenticationFilter에서 상속됨
        // 추가 필터 로직이 필요한 경우 여기에 구현
        super.doFilter(request, response, chain);
    }
}
            throws AuthenticationException {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                       Authentication Filter                   ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
        if (request.getCookies() != null) log.debug("request.getCookies() is not null");//jwtutility.deleteHttpOnlyCookie(request, response);

        // JSON 요청에서 사용자 로그인 정보 읽기
        loginRequest loginRequest;
        try {
            log.info("get loginRequest");
            loginRequest = objectMapper.readValue(request.getInputStream(), loginRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("login, userId : {}",loginRequest.getUserId());

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getUserId(), loginRequest.getPassword());

        log.info("login, authentication token created successfully");

        // 사용자 인증 시도
        Authentication authentication = null;
        try{
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e){
            log.error("authentication processing error", e);
        }

        if(!Objects.requireNonNull(authentication).isAuthenticated()){
            log.error("authentication failed");
            response.setStatus(401);
        }

        return authentication;
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) throws  IOException{

        principalDetails principalDetails = (principalDetails) authentication.getPrincipal();
        tokenDto tokendto = jwtUtility.buildToken(principalDetails.getUserId(),
                principalDetails.getNickname(), principalDetails.getAuthorities());

        log.info("Authentication successful for userId : {}", principalDetails.getUserId());
        log.info("Authentication token dto created");

        boolean RTokenSave = redisService.saveRToken(principalDetails.getUserId(), "server", tokendto.getRefreshToken());
        boolean accessTokenSave = redisService.saveAccessToken(tokendto.getRefreshToken(), tokendto.getAccessToken(), principalDetails.getUserId());
        log.info("redis save result R : {}, A : {} for userId {}", RTokenSave, accessTokenSave, principalDetails.getUserId());
        if (accessTokenSave && RTokenSave && jwtUtility.validateRefreshJWT(tokendto.getRefreshToken())){

            loginResponse(response, tokendto);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.OK.value());

            // JSON 형식으로 응답 생성 및 전송 (access_token)
            String jsonResponse = new ObjectMapper().writeValueAsString(Map.of(
                    "access_token", tokendto.getAccessToken()
            ));
            response.getWriter().write(jsonResponse);

        } else {
            log.error("REDIS token save failed return SERVER_INTERNAL_ERROR");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    private void loginResponse(HttpServletResponse response, tokenDto tokendto){
        Cookie newCookie = new Cookie("refreshToken", tokendto.getRefreshToken());
        newCookie.setHttpOnly(true);
        newCookie.setDomain(cookieDomain);
        newCookie.setPath("/");
        response.addCookie(newCookie);
        response.addHeader(SecurityConstants.TOKEN_HEADER,
                SecurityConstants.TOKEN_PREFIX + tokendto.getAccessToken());
    }

}

