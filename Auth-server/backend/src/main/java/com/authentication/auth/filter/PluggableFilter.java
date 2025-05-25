package com.authentication.auth.filter;

import jakarta.servlet.Filter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * 플러그형 필터 인터페이스
 * 동적으로 추가/제거 가능한 필터를 위한 인터페이스
 */
public interface PluggableFilter extends Filter {
    
    /**
     * HttpSecurity에 이 필터를 구성
     * @param http HttpSecurity 객체
     * @throws Exception 구성 중 예외 발생 시
     */
    void configure(HttpSecurity http) throws Exception;
    
    /**
     * 필터의 실행 순서 반환
     * 낮은 값이 높은 우선순위를 의미함
     * @return 필터 실행 순서
     */
    int getOrder();
    
    /**
     * 이 필터 이전에 실행되어야 하는 필터 클래스 반환
     * @return 이전 필터 클래스, 없으면 null
     */
    Class<? extends Filter> getBeforeFilter();
    
    /**
     * 이 필터 이후에 실행되어야 하는 필터 클래스 반환
     * @return 이후 필터 클래스, 없으면 null
     */
    Class<? extends Filter> getAfterFilter();
    
    /**
     * 필터 ID 반환 (기본 구현은 클래스명)
     * @return 필터 ID
     */
    default String getFilterId() {
        return this.getClass().getSimpleName();
    }
}
