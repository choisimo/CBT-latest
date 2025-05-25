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
 * 추상클래스 사용 이유 - reusability 향상상 (공통 기능 제공, 코드 중복 방지, 실행 순서, 흐름 관리리)
* @see org.springframework.web.filter.OncePerRequestFilter
 * 필터 실행 흐름 제어 및 공통 기능 제공
 */
@Slf4j
public abstract class AbstractSecurityFilter extends OncePerRequestFilter implements PluggableFilter {

    // 필터 실행 순서
    private final SecurityFilterOrder securityFilterOrder;

    /**
     * 생성자
     * @param securityFilterOrder 필터 실행 순서
     * @Description AbstractSecurityFilter의 생성자입니다. 필터의 실행 순서를 설정합니다.
     * protected 접근자 제한 - 하위 클래스에서만 사용 제약 
     */
    protected AbstractSecurityFilter(SecurityFilterOrder securityFilterOrder) {
        this.securityFilterOrder = securityFilterOrder;
    }

    @Override
    protected final void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // Check if the filter should be skipped based on subclass logic
        if (shouldSkipFilter(request)) {
            log.debug("필터 건너뜀: {} - {}", securityFilterOrder.name(), request.getRequestURI());
            chain.doFilter(request, response); // Continue with the chain without applying this filter
            return;
        }

        String path = request.getRequestURI();
        log.debug("필터 시작: {} - {}", securityFilterOrder.name(), path);
        
        try {
            // Call the new abstract method for actual filter logic
            doFilterLogic(request, response, chain);
            log.debug("필터 종료: {} - {}", securityFilterOrder.name(), path);
        } catch (Exception e) {
            log.error("필터 실행 중 오류 발생: {} - {}", securityFilterOrder.name(), e.getMessage(), e);
            handleFilterException(request, response, e);
        }
    }
    
    /**
     * 필터의 주요 비즈니스 로직 구현 (하위 클래스에서 구현)
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param chain 필터 체인
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     * @Description 필터의 주요 비즈니스 로직을 구현하는 메서드. 하위 클래스에서 반드시 구현해야 합니다.
     */
    protected abstract void doFilterLogic(HttpServletRequest request, HttpServletResponse response, 
                                           FilterChain chain) throws IOException, ServletException;
    
    /**
     * 필터 예외 처리 메서드
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param e 발생한 예외
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     * @Description 필터 처리 중 발생한 예외를 처리하는 메서드입니다. 기본적으로 예외를 다시 throw하며, 필요시 하위 클래스에서 재정의할 수 있습니다.
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
     * 필터의 실행 순서를 반환합니다.
     * @return int 필터 실행 순서 값
     * @Description {@link SecurityFilterOrder}에 정의된 순서 값을 반환합니다.
     */
    @Override
    public int getOrder() {
        return securityFilterOrder.getOrder();
    }
    
    /**
     * 현재 요청에 대해 필터를 건너뛸지 여부를 결정합니다.
     * @param request HTTP 요청
     * @return boolean 필터를 건너뛰려면 true, 그렇지 않으면 false
     * @Description 하위 클래스에서 이 메서드를 구현하여 특정 조건에 따라 필터 실행을 건너뛸 수 있도록 합니다.
     */
    protected abstract boolean shouldSkipFilter(HttpServletRequest request);
}
