package com.authentication.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SNS 로그인 요청 처리 필터
 * 소셜 미디어 인증 요청을 처리하고 리다이렉션
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnsRequestFilter extends AbstractSecurityFilter {

    private final ApiChecker apiChecker;

    public SnsRequestFilter(ApiChecker apiChecker) {
        super(SecurityFilterOrder.SNS_REQUEST_FILTER);
        this.apiChecker = apiChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        String requestURI = request.getRequestURI();
        
        // SNS 인증 관련 경로 처리
        if (requestURI.startsWith("/api/auth/social/")) {
            // SNS 제공자 추출 (예: /api/auth/social/google)
            String provider = requestURI.substring("/api/auth/social/".length());
            
            log.debug("SNS 인증 요청 감지: {}", provider);
            
            // SNS 별 인증 처리 로직 구현
            // 예: 요청 파라미터 검증, 리다이렉션 URL 생성 등
            
            // 인증 코드가 포함된 경우 토큰 교환 처리
            String code = request.getParameter("code");
            if (code != null && !code.isEmpty()) {
                log.debug("인증 코드 처리: {}", code);
                // 인증 코드로 액세스 토큰 교환 로직 구현
            }
        }
        
        // 다음 필터로 요청 전달
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // SNS 인증 경로만 필터링
        if (requestURI.startsWith("/api/auth/social/")) {
            return false; // 필터 적용
        }
        
        // 다른 모든 경로는 이 필터를 건너뜀
        return true;
    }
}
