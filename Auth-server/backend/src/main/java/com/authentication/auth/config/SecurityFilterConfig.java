package com.authentication.auth.config;

import com.authentication.auth.filter.AuthenticationFilter;
import com.authentication.auth.filter.AuthorizationFilter;
// import com.authentication.auth.filter.SnsRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 보안 필터 설정 클래스
 * 필터의 등록 및 순서 설정을 담당
 */
@Configuration
public class SecurityFilterConfig {

    private final AuthenticationFilter authenticationFilter;
    private final AuthorizationFilter authorizationFilter;

    public SecurityFilterConfig(@Lazy AuthenticationFilter authenticationFilter,
                                AuthorizationFilter authorizationFilter) {
        this.authenticationFilter = authenticationFilter;
        this.authorizationFilter = authorizationFilter;
    }


    // private final SnsRequestFilter snsRequestFilter;

    /**
     * 인증 필터 등록
     */
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration() {
        FilterRegistrationBean<AuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(authenticationFilter);
        // Disable direct servlet registration to avoid duplicate with Spring Security filter chain
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * 권한 필터 등록
     */
    @Bean
    public FilterRegistrationBean<AuthorizationFilter> authorizationFilterRegistration() {
        FilterRegistrationBean<AuthorizationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(authorizationFilter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * SNS 요청 필터 등록
     */
/* 
    @Bean
    public FilterRegistrationBean<SnsRequestFilter> snsRequestFilterRegistration() {
        FilterRegistrationBean<SnsRequestFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(snsRequestFilter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }
*/
}
