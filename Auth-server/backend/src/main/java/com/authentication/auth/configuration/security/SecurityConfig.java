package com.authentication.auth.configuration.security;

import com.authentication.auth.service.security.PrincipalDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.authentication.auth.filter.FilterRegistry;
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
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /***
     */
    private final CorsConfigurationSource corsConfigurationSource;
    private final PrincipalDetailService principalDetailService; // Used for non-OAuth login
    private final FilterRegistry filterRegistry;
    
    private final com.authentication.auth.configuration.security.handler.CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler; // Inject custom success handler
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          PrincipalDetailService principalDetailService,
                          FilterRegistry filterRegistry,
                          com.authentication.auth.configuration.security.handler.CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler,
                          OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.principalDetailService = principalDetailService;
        this.filterRegistry = filterRegistry;
        
        this.customOAuth2AuthenticationSuccessHandler = customOAuth2AuthenticationSuccessHandler;
        this.oauth2UserService = oauth2UserService;
    }

    // Define restriction arrays - initialize as empty, to be populated as needed
    private static final String[] userRestrict = {}; // Keep if these are for more specific role checks on secured paths
    private static final String[] adminRestrict = {}; // Keep if these are for more specific role checks on secured paths
    private static final String[] companyRestrict = {}; // Keep if these are for more specific role checks on secured paths
    // oauth2Restrict is removed as paths will be included in PUBLIC_URLS

    // Define all public URLs here
    private static final String[] PUBLIC_URLS = {
            "/login", "/error",
            "/h2-console/**",
            "/api/public/**", // Covers auth-related endpoints like /api/public/user/login, /api/public/user/register etc.
            // Swagger UI v2 paths from publicAPI.java
            "/v2/api-docs", "/swagger-resources", "/swagger-resources/**", // Added wildcard for /swagger-resources
            // Swagger UI v3 paths (common) - adding these for robustness
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", 
            "/webjars/**", // General webjars
            // OAuth2 paths (as per publicAPI.java and common practice)
            // Note: publicAPI.java had duplicates for google, removed here
            "/oauth2/authorization/google", "/oauth2/authorization/facebook", "/oauth2/authorization/github", 
            "/oauth2/authorization/linkedin", "/oauth2/authorization/instagram", "/oauth2/authorization/twitter", 
            "/oauth2/authorization/yahoo", "/oauth2/authorization/spotify", "/oauth2/authorization/amazon", 
            "/oauth2/authorization/microsoft", "/oauth2/authorization/okta", "/oauth2/authorization/slack",
            "/oauth2/callback/google", "/oauth2/callback/facebook", "/oauth2/callback/github", 
            "/oauth2/callback/linkedin", "/oauth2/callback/instagram", "/oauth2/callback/twitter", 
            "/oauth2/callback/yahoo", "/oauth2/callback/spotify", "/oauth2/callback/amazon", 
            "/oauth2/callback/microsoft", "/oauth2/callback/okta", "/oauth2/callback/slack",
            // Specific user paths from publicAPI.java (ensure these are actual public frontend routes or API endpoints)
            // If these are API endpoints, they should ideally be under /api/public/
            "/user/login", "/user/register", "/user/verify", "/user/forgetPassword", 
            "/user/resetPassword", "/user/verifyResetPassword", "/user/verifyEmail", 
            "/user/resendVerificationEmail",
            // Other public paths from publicAPI.java
            "/public", "/errorPage", "/notExist", "/unauthorized" 
            // Ensure these paths are distinct and correctly represent public resources.
            // Some like /unauthorized might be error views rather than pre-auth accessible endpoints.
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // cors
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        // 폼 로그인 비활성화
        http.formLogin(AbstractHttpConfigurer::disable);
        // Cross-Site Request Forgery 공격 방어 비활성화
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // Add this line
        // HTTP 기본 인증 비활성화
        http.httpBasic(AbstractHttpConfigurer::disable);
        // session 기반 로그인 비활성화
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // filter
        filterRegistry.configureFilters(http); // This custom registry might do other things

        
         // authorization
         http.authorizeHttpRequests((authorize) -> {
            authorize
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN")
                    .requestMatchers(userRestrict).hasAnyAuthority("ADMIN", "USER", "COMPANY") // Assuming these are more specific secured paths
                    .requestMatchers(adminRestrict).hasAnyAuthority("ADMIN")
                    .requestMatchers(companyRestrict).hasAnyAuthority("COMPANY", "ADMIN")
                    .anyRequest().authenticated(); // Secure all other requests
        });

        // The formLogin below is typically for stateful applications.
        // For a stateless API with JWT, this is usually not needed, as login is handled by a custom endpoint (e.g., /api/public/user/login).
        // If still using server-side rendered pages with Thymeleaf for login, it might be kept, but ensure it aligns with stateless strategy.
        // http.formLogin(formLogin -> formLogin
        //        .loginPage("/login").defaultSuccessUrl("/"));

        // 사용자 정보 서비스 및 암호화 설정 (for username/password flow)
        http.userDetailsService(principalDetailService);

        // OAUTH 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                // .loginPage("/login") // Optional: if you have a custom login page that triggers OAuth
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(oauth2UserService) // Custom OAuth2UserService
                )
                .successHandler(customOAuth2AuthenticationSuccessHandler) // Custom success handler for deep linking
                // .failureHandler(customOAuth2AuthenticationFailureHandler) // Optional: Custom failure handler
        );

        return http.build();
    }

    /**
     *  @apiNote BCryptPasswordEncoder Bean
     *  @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     *  @apiNote ObjectMapper Bean
     *  @return ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     *  @apiNote AuthenticationManager Bean
     *  @param authenticationConfiguration
     *  @return AuthenticationManager
     *  @throws Exception  
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
