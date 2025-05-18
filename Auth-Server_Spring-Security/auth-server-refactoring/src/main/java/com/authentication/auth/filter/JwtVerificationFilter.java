package com.authentication.auth.filter;

import com.authentication.auth.constants.ErrorType;
import com.authentication.auth.constants.FilterOrder;
import com.authentication.auth.constants.SecurityConstants;
import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.response.ApiResponse;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 토큰 검증 필터
 * 요청에 포함된 JWT 토큰을 검증하고 사용자 인증 정보를 설정
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
        super(FilterOrder.JWT_VERIFICATION);
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
                SecurityConstants.getPublicPaths().toArray(new String[0])
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
                    sendErrorResponse(response, ErrorType.INVALID_TOKEN);
                    return;
                }
            } else {
                log.debug("JWT 토큰이 없음");
            }
            
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT 검증 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED);
        }
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(SecurityConstants.TOKEN_HEADER.getValue());
        
        if (authHeader != null && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX.getValue())) {
            return authHeader.substring(SecurityConstants.TOKEN_PREFIX.getValue().length());
        }
        
        return null;
    }

    /**
     * 오류 응답 전송
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType) throws IOException {
        response.setStatus(errorType.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        objectMapper.writeValue(response.getOutputStream(), 
                               ApiResponse.error(errorType));
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
        return AuthorizationFilter.class;
    }
}
