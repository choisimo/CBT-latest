package com.authentication.auth.filter;

import com.authentication.auth.constants.ErrorType;
import com.authentication.auth.constants.FilterOrder;
import com.authentication.auth.constants.SecurityConstants;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * 권한 부여 필터
 * JWT 토큰의 유효성을 검증하고 사용자 권한을 검사하는 필터
 */
@Slf4j
@Component
public class AuthorizationFilter extends AbstractSecurityFilter {

    private final ObjectMapper objectMapper;
    private final FilterRegistry filterRegistry;
    private final Set<String> adminRoles = Set.of("ROLE_ADMIN", "ADMIN");

    @Autowired
    public AuthorizationFilter(
            ObjectMapper objectMapper,
            FilterRegistry filterRegistry) {
        super(FilterOrder.AUTHORIZATION);
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
                SecurityConstants.getPublicPaths().toArray(new String[0])
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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                // 관리자 전용 API 접근 제한
                if (path.startsWith(SecurityConstants.ADMIN_API_PATH.getValue()) && !hasAdminRole(authentication)) {
                    sendErrorResponse(response, ErrorType.ACCESS_DENIED);
                    return;
                }
                
                log.debug("사용자 '{}' 권한 확인 성공", authentication.getName());
            }
            
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("권한 부여 필터 실행 중 오류 발생: {}", e.getMessage());
            sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED);
        }
    }

    /**
     * 사용자가 관리자 권한을 가지고 있는지 확인
     */
    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> adminRoles.contains(authority.getAuthority()));
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
}
