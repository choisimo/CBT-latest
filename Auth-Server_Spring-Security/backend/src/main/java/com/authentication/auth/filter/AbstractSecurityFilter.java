package com.authentication.auth.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 모든 보안 필터의 기본 추상 클래스
 * 필터 실행 흐름 제어 및 공통 기능 제공
 */
@Slf4j
public abstract class AbstractSecurityFilter implements Filter {

    private final SecurityFilterOrder filterOrder;

    protected AbstractSecurityFilter(SecurityFilterOrder filterOrder) {
        this.filterOrder = filterOrder;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 필터 건너뛰기 여부 확인
        if (shouldNotFilter(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        
        try {
            log.debug("필터 시작: {}", filterOrder.name());
            doFilterInternal(httpRequest, httpResponse, chain);
            log.debug("필터 종료: {}", filterOrder.name());
        } catch (Exception e) {
            log.error("필터 실행 중 오류 발생: {}", filterOrder.name(), e);
            handleFilterException(httpRequest, httpResponse, e);
        }
    }
    
    /**
     * 필터의 주요 비즈니스 로직 구현
     */
    protected abstract void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                           FilterChain chain) throws IOException, ServletException;
    
    package com.authentication.auth.filter;
    
    import jakarta.servlet.Filter;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.ServletRequest;
    import jakarta.servlet.ServletResponse;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.web.filter.OncePerRequestFilter;
    
    import java.io.IOException;
    
    /**
     * @Author: choisimo
     * @Date: 2025-05-05
     * @Description: 추상 보안 필터
     * @Details: 모든 보안 필터의 기본 구현을 제공하는 추상 클래스
     */
    @Slf4j
    public abstract class AbstractSecurityFilter extends OncePerRequestFilter implements PluggableFilter {
    
        // 필터 실행 순서
        private final SecurityFilterOrder securityFilterOrder;
    
        /**
         * 생성자
         * @param securityFilterOrder 필터 실행 순서
         */
        protected AbstractSecurityFilter(SecurityFilterOrder securityFilterOrder) {
            this.securityFilterOrder = securityFilterOrder;
        }
    
        @Override
        public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        }
    
        /**
         * HTTP 요청에 대한 필터 처리
         * @param request HTTP 요청
         * @param response HTTP 응답
         * @param chain 필터 체인
         * @throws IOException IO 예외 발생 시
         * @throws ServletException 서블릿 예외 발생 시
         */
        private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            super.doFilter(request, response, chain);
        }
    
        @Override
        protected final boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
            String path = request.getRequestURI();
            boolean shouldNotFilter = shouldSkipFilter(request);
            
            if (shouldNotFilter) {
                log.trace("필터 건너뜀 ({}): {}", getFilterId(), path);
            } else {
                log.trace("필터 적용 ({}): {}", getFilterId(), path);
            }
            
            return shouldNotFilter;
        }
        
        /**
         * 필터 적용 여부 결정
         * @param request HTTP 요청
         * @return true인 경우 필터 실행 건너뜀
         */
        protected abstract boolean shouldSkipFilter(HttpServletRequest request);
    
        @Override
        public int getOrder() {
            return securityFilterOrder.getOrder();
        }
        
        @Override
        public String getFilterId() {
            return this.getClass().getSimpleName();
        }
    }
    
    /**
     * 필터 예외 처리 메서드
     */
    protected void handleFilterException(HttpServletRequest request, HttpServletResponse response, Exception e) 
            throws IOException, ServletException {
        // 기본 구현에서는 예외를 전파
        // 필요시 하위 클래스에서 재정의
        if (e instanceof IOException) {
            throw (IOException) e;
        } else if (e instanceof ServletException) {
            throw (ServletException) e;
        } else {
            throw new ServletException("필터 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 필터 순서 반환
     */
    public int getOrder() {
        return filterOrder.getOrder();
    }
}
