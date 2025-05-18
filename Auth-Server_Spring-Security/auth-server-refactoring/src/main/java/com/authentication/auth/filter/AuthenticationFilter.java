package com.authentication.auth.filter;

import com.authentication.auth.constants.ErrorType;
import com.authentication.auth.constants.FilterOrder;
import com.authentication.auth.constants.SecurityConstants;
import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.service.redis.RedisService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 인증 필터
 * 사용자 로그인 요청을 처리하고 JWT 토큰을 생성하는 필터
 */
@Slf4j
@Component
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter implements PluggableFilter {
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtility jwtUtility;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final String domain;
    private final String cookieDomain;
    private final FilterRegistry filterRegistry;
    
    @Autowired
    public AuthenticationFilter(
            AuthenticationManager authenticationManager,
            JwtUtility jwtUtility,
            ObjectMapper objectMapper,
            RedisService redisService,
            @Value("${site.domain}") String domain,
            @Value("${server.cookie.domain}") String cookieDomain,
            FilterRegistry filterRegistry) {
        this.authenticationManager = authenticationManager;
        this.jwtUtility = jwtUtility;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.domain = domain;
        this.cookieDomain = cookieDomain;
        this.filterRegistry = filterRegistry;
        setFilterProcessesUrl(SecurityConstants.LOGIN_PATH.getValue());
        setAuthenticationManager(authenticationManager);
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
                SecurityConstants.getPublicPaths().toArray(new String[0])
        );
        
        // 로그인 경로에는 POST 메소드만 허용하고 다른 메소드는 필터링
        PathPatternFilterCondition loginPathCondition = new PathPatternFilterCondition(
                "로그인 경로 메소드 제한",
                new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE},
                SecurityConstants.LOGIN_PATH.getValue()
        );
        
        // 필터 레지스트리에 조건 추가
        filterRegistry.addCondition(getFilterId(), publicApiCondition);
        filterRegistry.addCondition(getFilterId(), loginPathCondition);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws AuthenticationException {
        log.info("인증 시도: {}", request.getRequestURI());
        
        try {
            // 요청 바디에서 사용자 자격 증명 추출
            Map<String, String> credentials = objectMapper.readValue(request.getInputStream(), Map.class);
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            log.debug("사용자 인증 시도: {}", username);
            
            // 인증 토큰 생성 및 인증 시도
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(username, password);
                
            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            log.error("인증 요청 처리 중 오류 발생: {}", e.getMessage());
            throw new AuthenticationException(ErrorType.AUTHENTICATION_FAILED.getMessage()) {};
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult) 
                                           throws IOException, ServletException {
        log.info("인증 성공: 토큰 생성 시작");
        
        // 사용자 정보 추출
        String username = authResult.getName();
        
        // 토큰 생성
        TokenDto tokenDto = jwtUtility.createTokenDto(username, authResult.getAuthorities());
        
        // 리프레시 토큰을 Redis에 저장
        redisService.saveRToken(username, SecurityConstants.DEFAULT_PROVIDER.getValue(), tokenDto.refreshToken());
        redisService.saveAccessToken(tokenDto.refreshToken(), tokenDto.accessToken(), username);
        
        // 토큰을 쿠키에 저장
        Cookie refreshTokenCookie = new Cookie(SecurityConstants.COOKIE_REFRESH_TOKEN.getValue(), tokenDto.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath(SecurityConstants.COOKIE_PATH.getValue());
        refreshTokenCookie.setDomain(cookieDomain);
        refreshTokenCookie.setSecure(true);
        
        response.addCookie(refreshTokenCookie);
        response.addHeader(SecurityConstants.TOKEN_HEADER.getValue(), 
                          SecurityConstants.TOKEN_PREFIX.getValue() + tokenDto.accessToken());
        
        // 응답 본문에도 토큰 정보 포함
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", tokenDto.accessToken());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), 
                               ApiResponse.success(tokens, "로그인 성공"));
        
        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authResult);
        
        log.info("인증 성공: 토큰 생성 완료 및 쿠키 설정");
    }
    
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                             AuthenticationException failed) throws IOException, ServletException {
        log.error("인증 실패: {}", failed.getMessage());
        
        SecurityContextHolder.clearContext();
        
        response.setStatus(ErrorType.AUTHENTICATION_FAILED.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        objectMapper.writeValue(response.getOutputStream(), 
                               ApiResponse.error(ErrorType.AUTHENTICATION_FAILED));
    }
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAt(this, UsernamePasswordAuthenticationFilter.class);
        log.debug("인증 필터 HttpSecurity에 구성됨");
    }
    
    @Override
    public int getOrder() {
        return FilterOrder.AUTHENTICATION.getOrder();
    }
    
    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return null; // 이 필터 이전에 실행되어야 하는 필터가 없음
    }
    
    @Override
    public Class<? extends Filter> getAfterFilter() {
        return JwtVerificationFilter.class; // JWT 검증 필터 이전에 실행
    }
    
    @Override
    public String getFilterId() {
        return this.getClass().getSimpleName();
    }
}
