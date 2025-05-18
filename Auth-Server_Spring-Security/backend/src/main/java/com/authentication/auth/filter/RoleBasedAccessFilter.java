package com.authentication.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 역할 기반 접근 제어 필터
 * @Details: 더 세분화된 역할 기반 접근 제어를 구현한 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleBasedAccessFilter extends OncePerRequestFilter implements PluggableFilter {

    // URI 패턴에 따른 접근 제어 규칙 맵
    private final Map<Predicate<String>, Predicate<Authentication>> accessRules = new HashMap<>();
    
    // 생성자에서 접근 제어 규칙 초기화
    public RoleBasedAccessFilter() {
        // 관리자 전용 경로 설정
        accessRules.put(
            uri -> uri.startsWith("/admin"),
            auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
        );
        
        // 사용자 전용 경로 설정
        accessRules.put(
            uri -> uri.startsWith("/user"),
            auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER") || 
                                       a.getAuthority().equals("ROLE_ADMIN"))
        );
        
        // API 경로 설정
        accessRules.put(
            uri -> uri.startsWith("/api/reports"),
            auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ANALYST") || 
                                       a.getAuthority().equals("ROLE_ADMIN"))
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증되지 않은 요청은 다음 필터로 넘김 (인증 필터에서 처리)
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // URI에 맞는 접근 규칙 적용
        for (Map.Entry<Predicate<String>, Predicate<Authentication>> rule : accessRules.entrySet()) {
            if (rule.getKey().test(uri)) {
                if (!rule.getValue().test(authentication)) {
                    log.warn("역할 기반 접근 거부: 사용자={}, URI={}", 
                            authentication.getName(), uri);
                    throw new AccessDeniedException("해당 리소스에 접근할 권한이 없습니다.");
                }
                break;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAfter(this, AuthorizationFilter.class);
    }

    @Override
    public int getOrder() {
        return 400; // AuthorizationFilter 다음 순서
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return AuthorizationFilter.class;
    }

    @Override
    public Class<? extends Filter> getAfterFilter() {
        return null;
    }
}
