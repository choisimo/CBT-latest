package com.authentication.auth.config;

import com.authentication.auth.filter.AuthenticationFilter;
import com.authentication.auth.filter.AuthorizationFilter;
import com.authentication.auth.filter.SnsRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 보안 필터 설정 클래스
 * 필터의 등록 및 순서 설정을 담당
 */
@Configuration
@RequiredArgsConstructor
public class SecurityFilterConfig {

    private final AuthenticationFilter authenticationFilter;
    private final AuthorizationFilter authorizationFilter;
    private final SnsRequestFilter snsRequestFilter;

    /**
     * 인증 필터 등록
     */
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration() {
        FilterRegistrationBean<AuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(authenticationFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(authenticationFilter.getOrder());
        registrationBean.setName("authenticationFilter");
        return registrationBean;
    }

    /**
     * 권한 필터 등록
     */
    @Bean
    public FilterRegistrationBean<AuthorizationFilter> authorizationFilterRegistration() {
        FilterRegistrationBean<AuthorizationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(authorizationFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(authorizationFilter.getOrder());
        registrationBean.setName("authorizationFilter");
        return registrationBean;
    }

    /**
     * SNS 요청 필터 등록
     */
    @Bean
    public FilterRegistrationBean<SnsRequestFilter> snsRequestFilterRegistration() {
        FilterRegistrationBean<SnsRequestFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(snsRequestFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(snsRequestFilter.getOrder());
        registrationBean.setName("snsRequestFilter");
        return registrationBean;
    }
}
