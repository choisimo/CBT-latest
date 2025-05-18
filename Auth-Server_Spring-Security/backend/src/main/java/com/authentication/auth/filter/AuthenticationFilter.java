package com.authentication.auth.filter;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
package com.authentication.auth.filter;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 사용자 인증 필터
 * @Details: 사용자 로그인 요청을 처리하고 JWT 토큰을 생성하는 필터의 구체적인 구현
 *          유동적인 필터 조건 지원을 위해 개선됨
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractAuthenticationFilter {
    
    private final FilterRegistry filterRegistry;
    
    @Autowired
    public AuthenticationFilter(
            AuthenticationManager authenticationManager,
            JwtUtility jwtUtility,
            ObjectMapper objectMapper,
            RedisService redisService,
            String domain,
            String cookieDomain,
            FilterRegistry filterRegistry) {
        super(authenticationManager, jwtUtility, objectMapper, redisService, domain, cookieDomain);
        this.filterRegistry = filterRegistry;
        setFilterProcessesUrl("/api/auth/login"); // 로그인 URL 설정
    }
    
    /**
     * 필터 초기화 및 레지스트리에 등록
     */
    @PostConstruct
    public void init() {
        // 필터 레지스트리에 이 필터 등록
        filterRegistry.registerFilter(this);
        
        // 기본 필터 조건 설정 - 공개 API는 인증 필터 적용하지 않음
        PathPatternFilterCondition publicApiCondition = new PathPatternFilterCondition(
                "공개 API 경로 제외",
                "/api/public/**", 
                "/api/auth/register", 
                "/api/auth/refresh",
                "/swagger-ui/**", 
                "/v3/api-docs/**"
        );
        
        // 로그인 경로에는 POST 메소드만 허용하고 다른 메소드는 필터링
        PathPatternFilterCondition loginPathCondition = new PathPatternFilterCondition(
                "로그인 경로 메소드 제한",
                new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE},
                "/api/auth/login"
        );
        
        // 필터 레지스트리에 조건 추가
        filterRegistry.addCondition(getFilterId(), publicApiCondition);
        filterRegistry.addCondition(getFilterId(), loginPathCondition);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws AuthenticationException {
        // 필터 적용 여부 검사
        if (shouldNotFilter(request)) {
            log.debug("인증 필터 적용되지 않음: {}", request.getRequestURI());
            return null;
        }
        
        log.info("인증 시도: {}", request.getRequestURI());
        return super.attemptAuthentication(request, response);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult) throws IOException, ServletException {
        log.info("로그인 성공: 토큰 생성 시작");
        
        UserDetails userDetails = (UserDetails) authResult.getPrincipal();
        String username = userDetails.getUsername();
        
        // 액세스 및 리프레시 토큰 생성
        String accessToken = jwtUtility.generateAccessToken(username);
        String refreshToken = jwtUtility.generateRefreshToken(username);
        
        // 리프레시 토큰을 Redis에 저장
        redisService.setData("RT:" + username, refreshToken, jwtUtility.getRefreshTokenExpirationTime());
        
        // 토큰을 쿠키에 저장
        Cookie accessTokenCookie = new Cookie("access_token", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setDomain(cookieDomain);
        accessTokenCookie.setSecure(true);
        
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setDomain(cookieDomain);
        refreshTokenCookie.setSecure(true);
        
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
        
        // 응답 본문에도 토큰 정보 포함
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken);
        tokens.put("username", username);
        
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), tokens);
        
        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authResult);
        
        log.info("로그인 성공: 토큰 생성 완료 및 쿠키 설정");
    }
    
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                             AuthenticationException failed) throws IOException, ServletException {
        log.error("로그인 실패: {}", failed.getMessage());
        
        SecurityContextHolder.clearContext();
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "인증 실패");
        error.put("message", failed.getMessage());
        
        objectMapper.writeValue(response.getOutputStream(), error);
    }
    
    /**
     * 동적 필터 조건을 통해 필터 적용 여부 결정
     */
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return filterRegistry.shouldNotFilter(getFilterId(), request);
    }
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAt(this, AbstractAuthenticationFilter.class);
        log.debug("인증 필터 HttpSecurity에 구성됨");
    }
    
    @Override
    public int getOrder() {
        return SecurityFilterOrder.AUTHENTICATION_FILTER.getOrder();
    }
    
    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return null; // 이 필터 이전에 실행되어야 하는 필터가 없음
    }
    
    @Override
    public Class<? extends Filter> getAfterFilter() {
        return JwtVerificationFilter.class; // JWT 검증 필터 이전에 실행
    }
    
    /**
     * 필터에 새로운 조건 추가
     * @param condition 추가할 필터 조건
     */
    public void addFilterCondition(FilterCondition condition) {
        filterRegistry.addCondition(getFilterId(), condition);
    }
    
    /**
     * 필터에서 조건 제거
     * @param condition 제거할 필터 조건
     * @return 제거 성공 여부
     */
    public boolean removeFilterCondition(FilterCondition condition) {
        return filterRegistry.removeCondition(getFilterId(), condition);
    }
}
            String cookieDomain,
            ApiChecker apiChecker) {
        super(authenticationManager, jwtUtility, objectMapper, redisService, domain, cookieDomain);
        this.apiChecker = apiChecker;
        setFilterProcessesUrl("/api/auth/login"); // 로그인 URL 설정
    }
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws AuthenticationException {
        // 부모 클래스의 인증 메소드 호출
        return super.attemptAuthentication(request, response);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult) throws IOException, ServletException {
        log.info("로그인 성공: 토큰 생성 시작");
        
        UserDetails userDetails = (UserDetails) authResult.getPrincipal();
        String username = userDetails.getUsername();
        
        // 액세스 및 리프레시 토큰 생성
        String accessToken = jwtUtility.generateAccessToken(username);
        String refreshToken = jwtUtility.generateRefreshToken(username);
        
        // 리프레시 토큰을 Redis에 저장
        redisService.setData("RT:" + username, refreshToken, jwtUtility.getRefreshTokenExpirationTime());
        
        // 토큰을 쿠키에 저장
        Cookie accessTokenCookie = new Cookie("access_token", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setDomain(cookieDomain);
        accessTokenCookie.setSecure(true);
        
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setDomain(cookieDomain);
        refreshTokenCookie.setSecure(true);
        
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
        
        // 응답 본문에도 토큰 정보 포함
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken);
        
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), tokens);
        
        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authResult);
        
        log.info("로그인 성공: 토큰 생성 완료 및 쿠키 설정");
    }
    
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                             AuthenticationException failed) throws IOException, ServletException {
        log.error("로그인 실패: {}", failed.getMessage());
        
        SecurityContextHolder.clearContext();
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "인증 실패");
        error.put("message", failed.getMessage());
        
        objectMapper.writeValue(response.getOutputStream(), error);
    }
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAt(this, AbstractAuthenticationFilter.class);
    }
    
    @Override
    public int getOrder() {
        return 100; // 기본 인증 필터 순서
    }
    
    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return null; // 이 필터 이전에 실행되어야 하는 필터가 없음
    }
    
    @Override
    public Class<? extends Filter> getAfterFilter() {
        return JwtVerificationFilter.class; // JWT 검증 필터 이전에 실행
    }
}
    
    /**
     * extends와 implements 복수 사용에 관한 설명:
     * 
     * 1. 다중 상속 구조:
     *    - Java에서는 단일 상속만 가능합니다(extends는 한 클래스만 가능)
     *    - 하지만 복수의 인터페이스 구현(implements)은 가능합니다
     *    - 추상 클래스 예제에서는 UsernamePasswordAuthenticationFilter를 상속하면서
     *      PluggableFilter 인터페이스를 구현하는 방식을 사용했습니다
     * 
     * 2. 장점:
     *    - 상속(extends)을 통해 기존 클래스의 기능을 재사용
     *    - 인터페이스 구현(implements)을 통해 다양한 계약을 준수
     *    - 유연한 설계 가능: 하나의 클래스가 다양한 역할 수행 가능
     * 
     * 3. 단점:
     *    - 복잡한 상속 구조는 코드 이해를 어렵게 만들 수 있음
     *    - 다이아몬드 문제 발생 가능(상속 계층이 복잡할 때)
     *    - 상위 클래스 변경 시 하위 클래스에 영향
     * 
     * 4. 구체적 구현 클래스에서는:
     *    - AbstractSecurityFilter를 상속하는 더 단순한 구조 채택
     *    - 명확한 책임과 역할 분리로 유지보수성 향상
     */
