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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 권한 부여 필터
 * @Details: JWT 토큰의 유효성을 검증하고 사용자 권한을 검사하는 필터
 */
@Slf4j
@Component
public class authorizationFilter extends AbstractSecurityFilter {

    private final JwtUtility jwtUtility;
    private final ObjectMapper objectMapper;
    private final FilterRegistry filterRegistry;
    private final Set<String> adminRoles = Set.of("ROLE_ADMIN", "ADMIN");

    @Autowired
    public authorizationFilter(
            JwtUtility jwtUtility,
            ObjectMapper objectMapper,
            FilterRegistry filterRegistry) {
        super(SecurityFilterOrder.AUTHORIZATION_FILTER);
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

        // 기본 필터 조건 설정 - 공개 API는 권한 필터 적용하지 않음
        PathPatternFilterCondition publicApiCondition = new PathPatternFilterCondition(
                "권한 확인 불필요 경로",
                "/api/public/**",
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );

        // 관리자 전용 API는 ADMIN 역할 필요
        PathPatternFilterCondition adminApiCondition = new PathPatternFilterCondition(
                "관리자 전용 API",
                "/api/admin/**"
        );

        // 필터 레지스트리에 조건 추가
        filterRegistry.addCondition(getFilterId(), publicApiCondition);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        String path = request.getRequestURI();
        log.debug("권한 부여 필터 실행: {}", path);
        
        try {
            // 토큰에서 인증 정보 추출
            String token = extractToken(request);
            
            if (token != null && jwtUtility.validateToken(token)) {
                String username = jwtUtility.getUsernameFromToken(token);
                Set<String> roles = jwtUtility.getRolesFromToken(token);
                
                // 관리자 전용 API 접근 제한
                if (path.startsWith("/api/admin") && !hasAdminRole(roles)) {
                    sendForbiddenResponse(response, "관리자 권한이 필요합니다.");
                    return;
                }
                
                // 인증 정보 설정
                Authentication authentication = createAuthentication(username, roles);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("사용자 '{}' 인증 성공, 권한: {}", username, roles);
            }
            
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("권한 부여 필터 실행 중 오류 발생: {}", e.getMessage());
            sendErrorResponse(response, "인증 처리 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 이미 응답이 커밋되지 않은 경우에만 필터 체인 계속 진행
            if (!response.isCommitted()) {
                chain.doFilter(request, response);
            }
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
     * 사용자가 관리자 권한을 가지고 있는지 확인
     */
    private boolean hasAdminRole(Set<String> roles) {
        return roles.stream().anyMatch(adminRoles::contains);
    }

    /**
     * 인증 객체 생성
     */
    private Authentication createAuthentication(String username, Set<String> roles) {
        Set<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collections.toSet());
        
        return new UsernamePasswordAuthenticationToken(
                username, null, authorities);
    }

    /**
     * 권한 없음 응답 전송
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "접근 거부");
        error.put("message", message);
        
        objectMapper.writeValue(response.getOutputStream(), error);
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
        http.addFilterAfter(this, JwtVerificationFilter.class);
        log.debug("권한 부여 필터 HttpSecurity에 구성됨");
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return JwtVerificationFilter.class;
    }

    @Override
    public Class<? extends Filter> getAfterFilter() {
        return null;
    }

    /**
     * 필터에 새로운 조건 추가
     */
    public void addFilterCondition(FilterCondition condition) {
        filterRegistry.addCondition(getFilterId(), condition);
    }

    /**
     * 필터에서 조건 제거
     */
    public boolean removeFilterCondition(FilterCondition condition) {
        return filterRegistry.removeCondition(getFilterId(), condition);
    }
}
import com.authentication.auth.DTO.token.tokenDto;
import com.authentication.auth.DTO.users.loginRequest;
import com.authentication.auth.configuration.security.publicAPI;
import com.authentication.auth.configuration.token.jwtUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class authorizationFilter extends OncePerRequestFilter {

    @Value("${site.domain}")
    private String domain;

    @Value("${server.cookie.domain}")
    private String cookieDomain;

    @Value("${auth_proxy_header}")
    private String AuthorizedProxyHeader;


    private final jwtUtility  jwtUtility;
    private final redisService  redisService;
    private final ObjectMapper objectMapper;
    private final publicAPI apiChecker;
    public authorizationFilter(jwtUtility jwtUtility, redisService redisService, ObjectMapper objectMapper, publicAPI apiChecker) {
        this.jwtUtility = jwtUtility;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.apiChecker = apiChecker;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        // true 일 경우 filter 작동 skip.
        return apiChecker.checkRequestAPI(request);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Authorization Header 확인 : Bearer Token 확인
        String authorizationHeader = request.getHeader(SecurityConstants.TOKEN_HEADER);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid or missing Authorization header. Skipping filter.");
            return;
        }

        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                       Authorization Filter                    ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        String RToken = null;
        if (requiresCookieCheck(request)) {
            RToken = checkCookie(request, response);

            if (RToken == null) {
                log.error("cookie 필요 request 에 쿠키가 없어 401 오류를 반환합니다");
                return;
            }
        }

        String provider = request.getHeader("provider");

        if (provider != null) {
            log.info("{} 의 토큰입니다.", provider);
        }

        if (!authorizationHeader.startsWith(SecurityConstants.TOKEN_PREFIX)){

            if (Objects.requireNonNull(RToken).isEmpty()) {
                log.warn("refresh token 없음 -> 임시로 검사 안함 [수정 필요]");
                filterChain.doFilter(request, response);
                return;
            }

            // RToken 으로 AccessToken 찾기
            String findAccessToken = redisService.getAccessToken(RToken);

            if(findAccessToken != null && !findAccessToken.isEmpty()){
                response.addHeader(SecurityConstants.TOKEN_HEADER,
                        SecurityConstants.TOKEN_PREFIX + findAccessToken);
            }

        }

        String JWT = null;
        if(authorizationHeader.startsWith(SecurityConstants.TOKEN_PREFIX)){
            JWT = authorizationHeader.split(" ")[1];
        }

        if(JWT == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 기존 JWT 검증 및 인증 처리
        // Authorized Token 생성 및 Response Header 에 추가
        if (jwtUtility.validateJWT(JWT)) {
            log.info("유효한 토큰입니다.");
            Authentication getAuth = authenticateUser(JWT);

            log.info("인증 정보 : {}", getAuth);
            // 인증 실패 처리
            if (getAuth != null && getAuth.isAuthenticated()) {
                // 인증 성공: 필터 체인으로 요청 전달
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                // 인증 실패: 상태와 메시지 반환
                sendResponseStatus(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            }
            return;
        } else if (RToken == null) {
            log.error("there is no refreshToken in cookie");
            sendResponseStatus(response, HttpServletResponse.SC_UNAUTHORIZED, "no refresh token in cookie");
        } else if (jwtUtility.validateRefreshJWT(RToken)) {
            handleRefreshToken(request, response, filterChain, RToken, JWT);
        } else {
            sendResponseStatus(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
        }

        filterChain.doFilter(request, response);
    }

    private String checkCookie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Cookie[] cookies = request.getCookies();
        String RToken = null;
        if (cookies != null){
            for (Cookie cookie : cookies){
                if (cookie.getName().equals("refreshToken")){
                    RToken = cookie.getValue();
                    break;
                }
            }
        }
        if (RToken == null){
            sendResponseStatus(response, 403, "ㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠㅠ");
        }
        return RToken;
    }


    private void sendResponseStatus(HttpServletResponse response, int status, String message)
            throws IOException{
        response.setStatus(status);
        PrintWriter writer = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, String> responseBody = Collections.singletonMap("message", message);
        objectMapper.writeValue(response.getWriter(), responseBody);
    }

    private void sendFrontNewCookie(HttpServletResponse response, int status, tokenDto tokendto){
        response.setStatus(status);
        response.addHeader(SecurityConstants.TOKEN_HEADER, SecurityConstants.TOKEN_PREFIX + tokendto.getAccessToken());
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokendto.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setDomain(this.cookieDomain);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
    }

    private boolean RedisMatchRToken(String userId, String RToken){
        return redisService.findRToken(userId, "server", RToken);
    }


    private boolean requiresCookieCheck(HttpServletRequest request) {
        return !apiChecker.checkRequestAPI(request);
    }


    // JWT 토큰을 이용하여 사용자 인증 처리
    // Spring Security Context 에 인증 정보를 저장
    private Authentication authenticateUser(String JWT) {
        try {
            Authentication authentication = jwtUtility.getAuthentication(JWT);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return authentication;
        } catch (Exception e) {
            log.error("인증 실패");
            return null;
        }
    }

    private void handleRefreshToken(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String RToken, String JWT) throws IOException, ServletException {
        Map<String, Object> pToken = jwtUtility.getClaimsFromAccessToken(JWT);
        String userId = (String) pToken.get("userId");

        if (RedisMatchRToken(userId, RToken)) {
            log.info("refresh token is exist in redis");
            tokenDto tokendto = jwtUtility.buildToken(userId, (String) pToken.get("nickname"), (Collection<? extends GrantedAuthority>) pToken.get("role"));

            if (redisService.changeRToken(userId, "server", RToken, tokendto.getRefreshToken())) {
                log.info("access token and refresh token have been changed");
                sendFrontNewCookie(response, HttpServletResponse.SC_CREATED, tokendto);
                authenticateUser(tokendto.getAccessToken());
                filterChain.doFilter(request, response);
            } else {
                log.error("refresh token change failed");
                sendResponseStatus(response, HttpServletResponse.SC_UNAUTHORIZED, "refresh token change failed");
            }
        } else {
            log.error("refresh token from redis does not exist");
            sendResponseStatus(response, HttpServletResponse.SC_UNAUTHORIZED, "refresh token from redis does not exist");
        }
    }

}
