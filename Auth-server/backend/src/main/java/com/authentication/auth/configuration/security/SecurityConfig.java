package com.authentication.auth.configuration.security;

import com.authentication.auth.service.security.PrincipalDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final PrincipalDetailService principalDetailService;

    // Define restriction arrays - initialize as empty, to be populated as needed
    private static final String[] userRestrict = {};
    private static final String[] adminRestrict = {};
    private static final String[] companyRestrict = {};
    private static final String[] oauth2Restrict = {};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // cors
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        // 폼 로그인 비활성화
        http.formLogin(AbstractHttpConfigurer::disable);
        // Cross-Site Request Forgery 공격 방어 비활성화
        http.csrf(AbstractHttpConfigurer::disable);
        // HTTP 기본 인증 비활성화
        http.httpBasic(AbstractHttpConfigurer::disable);
        // session 기반 로그인 비활성화
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // filter
        // filterRegistry.registerFilters(http); // Commented out due to missing class

        // authorization
        http.authorizeHttpRequests((authorize) -> {
            authorize
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers(userRestrict).hasAnyAuthority("ADMIN", "USER", "COMPANY")
                    .requestMatchers(adminRestrict).hasAnyAuthority("ADMIN")
                    .requestMatchers(companyRestrict).hasAnyAuthority("COMPANY", "ADMIN")
                    .requestMatchers(oauth2Restrict).permitAll()
                    .anyRequest().permitAll();
        });

        http.formLogin(formLogin -> formLogin
                .loginPage("/login").defaultSuccessUrl("/"));

        // 사용자 정보 서비스 및 암호화 설정
        http.userDetailsService(principalDetailService);

        // OAUTH 로그인 설정
        // http.oauth2Login(login -> login.loginPage("/login"));

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
