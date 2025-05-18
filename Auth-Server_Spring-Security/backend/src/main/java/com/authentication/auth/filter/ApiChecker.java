package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 필터 적용 여부를 결정하는 API 요청 검사 클래스
 */
@Slf4j
@Component
public class ApiChecker {

    // 인증 필터를 적용하지 않을 공개 경로 목록
    private final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/swagger-ui",
        "/v3/api-docs",
        "/h2-console",
        "/error",
        "/favicon.ico"
    );

    /**
     * 요청이 필터를 적용하지 않아도 되는지 검사
     * @param request HTTP 요청
     * @return true일 경우 필터 적용 제외
     */
    public boolean checkRequestAPI(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // 허용된 경로인지 확인
        for (String path : PUBLIC_PATHS) {
            if (requestURI.startsWith(path)) {
                log.debug("필터 적용 제외 경로: {}", requestURI);
                return true;
            }
        }
        
        // OPTIONS 메서드는 필터링 제외 (CORS preflight 요청)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        return false;
    }
}
