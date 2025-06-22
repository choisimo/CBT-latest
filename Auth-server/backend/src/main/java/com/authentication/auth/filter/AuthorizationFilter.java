package com.authentication.auth.filter;

import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.others.constants.SecurityConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 권한 부여 필터
 * 관리자 API에 대한 접근 권한을 검사하는 필터
 */
@Slf4j
@Component
public class AuthorizationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final Set<String> adminRoles = Set.of("ROLE_ADMIN", "ADMIN");

    public AuthorizationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 관리자 API 경로가 아니면 필터를 통과시킵니다.
        if (!path.startsWith(SecurityConstants.ADMIN_API_PATH.getValue())) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("관리자 권한 부여 필터 실행: {}", path);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나, 인증되지 않았거나, 관리자 역할이 없는 경우 접근을 거부합니다.
        if (authentication == null || !authentication.isAuthenticated() || !hasAdminRole(authentication)) {
            log.warn("사용자 '{}'가 관리자 API에 접근 시도: {}", (authentication != null ? authentication.getName() : "anonymous"), path);
            sendErrorResponse(response, ErrorType.FORBIDDEN);
            return;
        }

        log.debug("사용자 '{}' 관리자 권한 확인 성공", authentication.getName());
        filterChain.doFilter(request, response);
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
        if (response.isCommitted()) {
            log.warn("Response already committed, skipping error response for {}", errorType);
            return;
        }
        response.setStatus(errorType.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(),
                               ApiResponse.error(errorType));
    }
}
