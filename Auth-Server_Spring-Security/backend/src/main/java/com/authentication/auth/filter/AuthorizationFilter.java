package com.authentication.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 인가 필터
 * @Details: 사용자의 권한에 따라 특정 리소스 접근 제어를 담당하는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter implements PluggableFilter {

    // URL 패턴별 필요 권한 매핑
    private final Map<RequestMatcher, List<String>> urlAuthorizationMap = new HashMap<>();
    
    // 생성자에서 URL 패턴별 필요 권한 초기화
    public AuthorizationFilter() {
        // 예시: 관리자 전용 URL 패턴
        urlAuthorizationMap.put(
            new AntPathRequestMatcher("/admin/**"),
            List.of("ROLE_ADMIN")
        );
        
        // 예시: 사용자 및 관리자 접근 가능 URL 패턴
        urlAuthorizationMap.put(
            new AntPathRequestMatcher("/api/users/**"),
            List.of("ROLE_USER", "ROLE_ADMIN")
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증 정보가 없거나 익명 사용자인 경우 바로 통과 (인증 필터에서 처리)
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // URL 패턴별 권한 확인
        for (Map.Entry<RequestMatcher, List<String>> entry : urlAuthorizationMap.entrySet()) {
            if (entry.getKey().matches(request)) {
                if (!hasAnyAuthority(authentication.getAuthorities(), entry.getValue())) {
                    log.warn("접근 권한 없음. 사용자: {}, URL: {}", authentication.getName(), request.getRequestURI());
                    throw new AccessDeniedException("접근 권한이 없습니다.");
                }
                break;
            }
        }
        
        // 권한 검사를 통과하면 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
    
    // 사용자가 필요한 권한 중 하나라도 가지고 있는지 확인
    private boolean hasAnyAuthority(Collection<? extends GrantedAuthority> authorities, List<String> requiredAuthorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(requiredAuthorities::contains);
    }
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAfter(this, JwtVerificationFilter.class);
    }

    @Override
    public int getOrder() {
        return 300; // JwtVerificationFilter 다음 순서
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return JwtVerificationFilter.class;
    }

    @Override
    public Class<? extends Filter> getAfterFilter() {
        return null; // 이 필터 이후에 실행되어야 하는 특정 필터가 없음
    }
}
