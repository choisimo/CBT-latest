package com.authentication.auth.filter;
package com.authentication.auth.filter;

import com.authentication.auth.configuration.token.JwtUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: JWT 토큰 검증 필터
 * @Details: 요청에 포함된 JWT 토큰을 검증하고 사용자 인증 정보를 설정
 */
@Slf4j
@Component
public class JwtVerificationFilter extends AbstractSecurityFilter {

    private final JwtUtility jwtUtility;
    private final ObjectMapper objectMapper;
    private final FilterRegistry filterRegistry;

    @Autowired
    public JwtVerificationFilter(
            JwtUtility jwtUtility,
            ObjectMapper objectMapper,
            FilterRegistry filterRegistry) {
        super(SecurityFilterOrder.JWT_VERIFICATION_FILTER);
        this.jwtUtility = jwtUtility;
        this.objectMapper = objectMapper;
        this.filterRegistry = filterRegistry;
    }

    /**
     * 필터 초기화 및 레지스트리에 등록
     */
    @PostConstruct
    public void init() {
        // 필터 레지스트리에 이 필터 등록
        filterRegistry.registerFilter(this);

        // 기본 필터 조건 설정 - 공개 API는 JWT 검증 필터 적용하지 않음
        PathPatternFilterCondition publicApiCondition = new PathPatternFilterCondition(
                "JWT 검증 불필요 경로",
                "/api/public/**",
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );

        // 필터 레지스트리에 조건 추가
        filterRegistry.addCondition(getFilterId(), publicApiCondition);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        String path = request.getRequestURI();
        log.debug("JWT 검증 필터 실행: {}", path);
        
        try {
            String token = extractToken(request);
            
            if (token != null) {
                if (jwtUtility.validateToken(token)) {
                    String username = jwtUtility.getUsernameFromToken(token);
                    Set<String> roles = jwtUtility.getRolesFromToken(token);
                    
                    // 권한 설정
                    Set<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toSet());
                    
                    UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT 토큰 검증 성공: {}", username);
                } else {
                    log.warn("유효하지 않은 JWT 토큰");
                    sendErrorResponse(response, "유효하지 않은 토큰입니다.");
                    return;
                }
            } else {
                log.debug("JWT 토큰이 없음");
            }
            
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT 검증 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, "토큰 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    /**
     * 오류 응답 전송
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "인증 오류");
        error.put("message", message);
        
        objectMapper.writeValue(response.getOutputStream(), error);
    }

    @Override
    protected boolean shouldSkipFilter(HttpServletRequest request) {
        return filterRegistry.shouldNotFilter(getFilterId(), request);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAfter(this, AuthenticationFilter.class);
        log.debug("JWT 검증 필터 HttpSecurity에 구성됨");
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return AuthenticationFilter.class;
    }

    @Override
    public Class<? extends Filter> getAfterFilter() {
        return authorizationFilter.class;
    }
}
import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.utility.JwtUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.auth.login.CredentialExpiredException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: JWT 토큰 검증 필터
 * @Details: 요청에서 JWT 토큰을 추출하고 유효성을 검사하는 필터
 * @Usage: Spring Security 필터 체인에 등록하여 사용
 */
@Slf4j
@RequiredArgsConstructor
public class JwtVerificationFilter extends OncePerRequestFilter implements PluggableFilter {
    
    private final JwtUtility jwtUtility;
    private final RedisService redisService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;
    private final RequestMatcher excludePathMatcher;

    /**
     * JWT 검증 필터 생성자
     * @param jwtUtility JWT 토큰 유틸리티
     * @param redisService Redis 서비스
     * @param userDetailsService 사용자 세부정보 서비스
     * @param objectMapper JSON 변환용 객체 매퍼
     * @param excludePaths 필터 적용을 제외할 경로 목록
     */
    public JwtVerificationFilter(JwtUtility jwtUtility, RedisService redisService, 
                                UserDetailsService userDetailsService, ObjectMapper objectMapper,
                                List<String> excludePaths) {
        this.jwtUtility = jwtUtility;
        this.redisService = redisService;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
        
        // 제외 경로 패턴 매처 설정
        List<RequestMatcher> matchers = excludePaths.stream()
                .map(path -> new AntPathRequestMatcher(path))
                .collect(Collectors.toList());
        this.excludePathMatcher = new OrRequestMatcher(matchers.toArray(new RequestMatcher[0]));
    }

    /**
     * 필터 적용 여부 결정 메서드
     * 특정 경로는 JWT 검증에서 제외
     * @param request 현재 요청
     * @return 필터 적용 여부
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return excludePathMatcher.matches(request);
    }
        
        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterBefore(this, AuthorizationFilter.class);
        }
    
        @Override
        public int getOrder() {
            return 200; // AuthenticationFilter 다음 순서
        }
    
        @Override
        public Class<? extends Filter> getBeforeFilter() {
            return AuthenticationFilter.class;
        }
    
        @Override
        public Class<? extends Filter> getAfterFilter() {
            return AuthorizationFilter.class;
        }

    /**
     * JWT 토큰 검증 및 인증 처리 메서드
     * @param request 요청
     * @param response 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException IO 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        log.debug("JWT 검증 필터 시작: {}", request.getRequestURI());
        
        try {
            // 토큰 추출
            String token = extractToken(request);
            
            if (token == null) {
                log.debug("토큰이 없음, 인증 생략");
                filterChain.doFilter(request, response);
                return;
            }
            
            // 토큰 유효성 검증
            if (jwtUtility.validateToken(token)) {
                // 토큰에서 사용자 정보 추출
                Claims claims = jwtUtility.getClaims(token);
                String username = claims.getSubject();
                
                // 블랙리스트 확인 (로그아웃된 토큰인지)
                String tokenId = claims.getId();
                if (tokenId != null && redisService.hasKey("JWT_Blacklist_" + tokenId)) {
                    throw new CredentialExpiredException("로그아웃된 토큰입니다.");
                }
                
                // 사용자 권한 추출
                List<String> authorities = claims.get("authorities", List.class);
                List<SimpleGrantedAuthority> grantedAuthorities = null;
                
                if (authorities != null) {
                    grantedAuthorities = authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }
                
                // 인증 객체 생성 및 설정
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(username, null, grantedAuthorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 보안 컨텍스트에 인증 객체 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("사용자 {} 인증 완료", username);
            }
            
            // 필터 체인 계속 진행
            filterChain.doFilter(request, response);
            
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
            handleExpiredToken(request, response, e);
        } catch (JwtException | CredentialExpiredException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
            handleInvalidToken(response, e);
        }
    }

    /**
     * 요청에서 JWT 토큰 추출 메서드
     * Authorization 헤더 또는 쿠키에서 토큰 추출
     * @param request HTTP 요청
     * @return 추출된 토큰 또는 null
     */
    private String extractToken(HttpServletRequest request) {
        // Authorization 헤더에서 토큰 추출 시도
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 쿠키에서 토큰 추출 시도
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> "access_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        
        return null;
    }

    /**
     * 만료된 토큰 처리 메서드
     * 리프레시 토큰을 사용하여 새 액세스 토큰 발급 시도
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param e 만료 예외
     * @throws IOException IO 예외
     */
    private void handleExpiredToken(HttpServletRequest request, HttpServletResponse response, ExpiredJwtException e) 
            throws IOException {
        log.info("만료된 토큰 처리 시도");
        
        // 리프레시 토큰 추출
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            Optional<Cookie> refreshCookie = Arrays.stream(cookies)
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .findFirst();
                    
            if (refreshCookie.isPresent()) {
                refreshToken = refreshCookie.get().getValue();
            }
        }
        
        // 리프레시 토큰이 없거나 유효하지 않은 경우
        if (refreshToken == null || !jwtUtility.validateToken(refreshToken)) {
            handleInvalidToken(response, e);
            return;
        }
        
        try {
            // 리프레시 토큰에서 정보 추출
            Claims refreshClaims = jwtUtility.getClaims(refreshToken);
            String username = refreshClaims.getSubject();
            String tokenId = refreshClaims.getId();
            
            // Redis에서 리프레시 토큰 유효성 검증
            String redisKey = "JWT_RToken_" + username + "_" + tokenId;
            if (!redisService.hasKey(redisKey)) {
                throw new JwtException("유효하지 않은 리프레시 토큰");
            }
            
            // 사용자 정보 로드 및 새 액세스 토큰 생성
            var userDetails = userDetailsService.loadUserByUsername(username);
            String newAccessToken = jwtUtility.generateAccessToken(username, userDetails.getAuthorities());
            
            // 새 액세스 토큰을 쿠키에 설정
            Cookie newAccessTokenCookie = new Cookie("access_token", newAccessToken);
            newAccessTokenCookie.setHttpOnly(true);
            newAccessTokenCookie.setSecure(true);
            newAccessTokenCookie.setPath("/");
            newAccessTokenCookie.setMaxAge((int) (jwtUtility.getAccessTokenExpiration() / 1000));
            response.addCookie(newAccessTokenCookie);
            
            // 클라이언트에게 토큰 갱신 응답
            Map<String, Object> tokenResponse = new HashMap<>();
            tokenResponse.put("status", "renewed");
            tokenResponse.put("message", "액세스 토큰이 갱신되었습니다");
            tokenResponse.put("timestamp", LocalDateTime.now().toString());
            
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.OK.value());
            objectMapper.writeValue(response.getWriter(), tokenResponse);
            
            log.info("액세스 토큰 갱신 완료: 사용자 {}", username);
            
        } catch (Exception refreshError) {
            log.error("토큰 갱신 실패: {}", refreshError.getMessage());
            handleInvalidToken(response, refreshError);
        }
    }

    /**
     * 유효하지 않은 토큰 처리 메서드
     * 인증 오류 응답 반환
     * @param response HTTP 응답
     * @param e 예외
     * @throws IOException IO 예외
     */
    private void handleInvalidToken(HttpServletResponse response, Exception e) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "인증 오류: " + e.getMessage());
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
        http.addFilterBefore(this, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * PluggableFilter 인터페이스 구현
     * 필터 순서 반환 메서드
     */
    @Override
    public int getOrder() {
        return 1; // JWT 검증은 다른 필터보다 먼저 실행되어야 함
    }

    /**
     * PluggableFilter 인터페이스 구현
     * 이 필터 이전에 적용될 필터 클래스 반환
     */
    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return null; // 이 필터 이전에 적용될 특정 필터가 없음
    }

    /**
     * PluggableFilter 인터페이스 구현
     * 이 필터 이후에 적용될 필터 클래스 반환
     */
    @Override
    public Class<? extends Filter> getAfterFilter() {
        return UsernamePasswordAuthenticationFilter.class;
    }
}
