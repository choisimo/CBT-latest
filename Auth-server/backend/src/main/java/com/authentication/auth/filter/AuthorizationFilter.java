package com.authentication.auth.filter;

import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.others.constants.SecurityConstants;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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

    /**
     * AuthorizationFilter 생성자
     * @param objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
     * @param filterRegistry 필터 레지스트리
     * @Description 의존성 주입 및 부모 클래스 생성자 호출 (필터 순서 설정)
     */
    @Autowired
    public AuthorizationFilter(
            ObjectMapper objectMapper,
            FilterRegistry filterRegistry) {
        super(SecurityFilterOrder.AUTHORIZATION_FILTER);
        this.objectMapper = objectMapper;
        this.filterRegistry = filterRegistry;
    }

    /**
     * 필터 초기화 및 레지스트리에 등록
     * @Description AuthorizationFilter를 FilterRegistry에 등록하고, 필터링 조건을 설정합니다.
     *              공개 API 경로는 권한 필터 적용에서 제외합니다.
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

    /**
     * 필터의 주요 비즈니스 로직 구현 (권한 검사)
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param chain 필터 체인
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     * @Description SecurityContextHolder에서 인증 정보를 가져와 관리자 API 접근 권한 등을 확인합니다.
     */
    @Override
    protected void doFilterLogic(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        String path = request.getRequestURI();
        log.debug("권한 부여 필터 실행: {}", path);
        
        try {
            // 토큰에서 인증 정보 추출
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                // 관리자 전용 API 접근 제한
                if (path.startsWith(SecurityConstants.ADMIN_API_PATH.getValue()) && !hasAdminRole(authentication)) {
                    sendErrorResponse(response, ErrorType.FORBIDDEN);
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
     * @param authentication 인증 객체
     * @return boolean 관리자 권한이 있으면 true, 그렇지 않으면 false
     * @Description 주어진 Authentication 객체의 권한 목록을 확인하여 관리자 역할(ROLE_ADMIN 또는 ADMIN)이 있는지 검사합니다.
     */
    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> adminRoles.contains(authority.getAuthority()));
    }

    /**
     * 오류 응답 전송
     * @param response HTTP 응답
     * @param errorType 오류 유형
     * @throws IOException 입출력 예외
     * @Description 지정된 오류 유형으로 API 응답을 생성하여 클라이언트에게 전송합니다.
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType) throws IOException {
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
        return "authorizationFilter";
    }

    /**
     * HttpSecurity에 필터 구성 (AbstractSecurityFilter를 상속하므로, configure 메서드는 일반적으로 직접 호출되지 않음)
     * @param http HttpSecurity 객체
     * @throws Exception 구성 중 예외 발생 시
     * @Description 이 필터는 AbstractSecurityFilter를 상속하며, FilterRegistry를 통해 관리됩니다.
     *              HttpSecurity에 직접 추가하는 대신 FilterChainProxy에 의해 동적으로 추가됩니다.
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 이 필터는 FilterRegistry를 통해 관리되며, HttpSecurity에 직접 추가되지 않음
    }

    /**
     * 필터 적용 여부 결정
     * @param request HTTP 요청
     * @return boolean 필터를 적용하지 않으려면 true, 적용하려면 false
     * @Description FilterRegistry에 등록된 조건들을 확인하여 현재 요청에 이 필터를 적용할지 여부를 결정합니다.
     */
    @Override
    public boolean shouldSkipFilter(HttpServletRequest request) {
        return filterRegistry.shouldApplyFilter(getFilterId(), request);
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
