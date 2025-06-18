package com.authentication.auth.filter;

import com.authentication.auth.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.filter.SecurityFilterOrder;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.configuration.token.JwtUtility;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import java.io.IOException;

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

    /**
     * JwtVerificationFilter 생성자
     * @param jwtUtility JWT 토큰 생성 및 검증 유틸리티
     * @param objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
     * @param filterRegistry 필터 레지스트리
     * @Description 의존성 주입 및 부모 클래스 생성자 호출 (필터 순서 설정)
     */
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
     * @Description JwtVerificationFilter를 FilterRegistry에 등록하고, 필터링 조건을 설정합니다.
     *              공개 API 경로는 JWT 검증 필터 적용에서 제외합니다.
     */
    @PostConstruct
    public void init() {
        if (filterRegistry != null) {
            filterRegistry.registerFilter(this);
            // Example: Add conditions if this filter should skip certain paths
            // filterRegistry.addCondition(getFilterId(), new PathRequestCondition("/api/public/**"));
        } else {
            log.warn("FilterRegistry is null. JWTVerificationFilter may not be registered correctly.");
        }
    }

    /**
     * 필터의 주요 비즈니스 로직 구현 (JWT 검증)
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     * @Description 요청 헤더에서 JWT 토큰을 추출하여 유효성을 검증하고, 유효한 경우 SecurityContextHolder에 인증 정보를 설정합니다.
     */
    @Override
    protected void doFilterLogic(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        
        String path = request.getRequestURI();
        log.debug("JWT 검증 필터 실행: {}", path);
        
        try {
            String token = extractToken(request);

            if (token != null && jwtUtility.validateJWT(token)) {
                Authentication authentication = jwtUtility.getAuthentication(token);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", authentication.getName(), request.getRequestURI());
                } else {
                    log.debug("유효한 JWT 토큰이지만, 인증 정보 생성에 실패했습니다. uri: {}", request.getRequestURI());
                    // Depending on policy, might send an error or allow request to proceed unauthenticated
                }
            } else if (token == null) {
                log.debug("JWT 토큰이 없습니다, uri: {}", request.getRequestURI());
                // If token is required for the path and is missing, an error might be appropriate.
                // However, shouldNotFilter should handle public paths. If it reaches here and token is null,
                // it might be an implicitly protected path or a misconfiguration.
                // For now, let it proceed, relying on subsequent security mechanisms or endpoint-specific checks.
            } else { // Token is not null but invalid
                log.warn("유효하지 않은 JWT 토큰입니다, uri: {}", request.getRequestURI());
                sendErrorResponse(response, ErrorType.INVALID_TOKEN);
                return; // Stop processing if token is invalid
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다, uri: {}: {}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(response, ErrorType.EXPIRED_TOKEN);
        } catch (JwtException e) {
            log.error("JWT 처리 중 오류 발생, uri: {}: {}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(response, ErrorType.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("JwtVerificationFilter 처리 중 예기치 않은 오류 발생, uri: {}: {}", request.getRequestURI(), e.getMessage(), e);
            //sendErrorResponse(response, ErrorType.INTERNAL_SERVER_ERROR);
            throw e;
        }
    }

    /**
     * 요청에서 JWT 토큰 추출
     * @param request HTTP 요청
     * @return String 추출된 JWT 토큰 (없거나 형식이 맞지 않으면 null)
     * @Description HTTP 요청의 Authorization 헤더에서 'Bearer ' 접두사를 가진 JWT 토큰을 추출합니다.
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
     * @param response HTTP 응답
     * @param errorType 오류 유형
     * @throws IOException 입출력 예외
     * @Description 지정된 오류 유형으로 API 응답을 생성하여 클라이언트에게 전송합니다.
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType) throws IOException {
        if (response.isCommitted()) {
            // Another component (e.g., Spring error handler) already started the response
            log.warn("Response already committed, skipping error response for {}", errorType);
            return;
        }
        response.setStatus(errorType.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        objectMapper.writeValue(response.getOutputStream(), 
                               ApiResponse.error(errorType));
    }

    /**
     * 필터 ID 반환
     * @return String 필터의 고유 ID
     * @Description 이 필터의 고유 ID를 반환합니다. 일반적으로 클래스 이름으로 설정됩니다.
     */
    @Override
    public String getFilterId() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 현재 요청에 대해 필터를 건너뛸지 여부를 결정합니다.
     * @param request HTTP 요청
     * @return boolean 필터를 건너뛰려면 true, 그렇지 않으면 false
     * @Description 이 필터가 현재 요청에 대해 건너뛸지 여부를 결정합니다.
     */
    @Override
    protected boolean shouldSkipFilter(HttpServletRequest request) {
        String path = request.getRequestURI(); // Keep for logging if needed
        log.trace("JwtVerificationFilter.shouldSkipFilter called for path: {}", path);
        // Delegate to FilterRegistry to determine if this filter should be skipped
        // based on its configured conditions.
        // filterRegistry.shouldApplyFilter returns true if any condition wants to skip the filter.
        return filterRegistry.shouldApplyFilter(getFilterId(), request);
    }

    /**
     * 이 필터가 실행되어야 하는 필터 클래스를 반환 (이전 필터)
     * @return Class<? extends Filter> 이전 필터 클래스
     * @Description 이 필터가 {@link AuthenticationFilter} 이후에 실행되어야 함을 나타냅니다.
     *              FilterRegistry에서 필터 순서를 결정하는 데 사용될 수 있습니다.
     */
    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return AuthenticationFilter.class;
    }

    /**
     * 이 필터 다음에 실행되어야 하는 필터 클래스를 반환 (다음 필터)
     * @return Class<? extends Filter> 다음 필터 클래스
     * @Description 이 필터가 {@link AuthorizationFilter} 이전에 실행되어야 함을 나타냅니다.
     *              FilterRegistry에서 필터 순서를 결정하는 데 사용될 수 있습니다.
     */
    @Override
    public Class<? extends Filter> getAfterFilter() {
        return AuthorizationFilter.class;
    }

    /**
     * HttpSecurity에 필터 구성
     * @param http HttpSecurity 객체
     * @throws Exception 구성 중 예외 발생 시
     * @Description 이 필터를 AuthenticationFilter 뒤에 추가하도록 HttpSecurity를 설정합니다.
     *              FilterRegistry를 통해 필터가 관리되므로, 이 메서드는 일반적으로 직접 호출되지 않거나,
     *              Spring Security 설정 시 FilterRegistry의 configureFilters를 통해 간접적으로 영향을 줄 수 있습니다.
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // This filter is for JWT verification, actual path protection rules
        // are typically defined in a central SecurityFilterChain configuration.
        // If this filter had specific responsibilities for HttpSecurity, they would go here.
        // For now, we assume it's part of a larger configuration.
        log.debug("JwtVerificationFilter.configure(HttpSecurity) called. No specific configurations applied by this filter directly.");
    }
}
