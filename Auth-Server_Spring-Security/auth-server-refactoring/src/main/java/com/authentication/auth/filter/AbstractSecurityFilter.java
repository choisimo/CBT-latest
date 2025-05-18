package com.authentication.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 보안 필터의 기본 추상 클래스
 * 필터 실행 흐름 제어 및 공통 기능 제공
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
    protected final void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        log.debug("필터 시작: {} - {}", securityFilterOrder.name(), path);
        
        try {
            doFilterInternal(request, response, chain);
            log.debug("필터 종료: {} - {}", securityFilterOrder.name(), path);
        } catch (Exception e) {
            log.error("필터 실행 중 오류 발생: {} - {}", securityFilterOrder.name(), e.getMessage(), e);
            handleFilterException(request, response, e);
        }
    }
    
    /**
     * 필터의 주요 비즈니스 로직 구현
     */
    protected abstract void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                           FilterChain chain) throws IOException, ServletException;
    
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
    
    @Override
    public int getOrder() {
        return securityFilterOrder.getOrder();
    }
    
    /**
     * 필터 적용 여부 결정 메서드
     * @param request HTTP 요청
     * @return 필터 적용 여부
     */
    protected abstract boolean shouldSkipFilter(HttpServletRequest request);
}
