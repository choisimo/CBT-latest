package com.authentication.auth.configuration.security;

import com.authentication.auth.service.security.PrincipalDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.authentication.auth.filter.AuthenticationFilter;
import com.authentication.auth.filter.AuthorizationFilter;
import com.authentication.auth.filter.JwtVerificationFilter;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.service.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
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
    private final JwtUtility jwtUtility;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Value("${server.cookie.domain}")
    private String cookieDomain;

    @Value("${ACCESS_TOKEN_VALIDITY}")
    private int accessTokenValidity;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          PrincipalDetailService principalDetailService,
                          JwtUtility jwtUtility,
                          RedisService redisService,
                          ObjectMapper objectMapper) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.principalDetailService = principalDetailService;
        this.jwtUtility = jwtUtility;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
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
            "/public", "/errorPage", "/notExist", "/unauthorized",
            // Debug endpoints (TODO: Remove in production)
            "/api/diaries/*/analysis/debug-trigger",
            // SSE endpoint for real-time updates
            "/subscribe",
            // Email verification endpoints (explicitly added for clarity)
            "/api/public/emailCode", "/api/public/emailCheck"
            // Ensure these paths are distinct and correctly represent public resources.
            // Some like /unauthorized might be error views rather than pre-auth accessible endpoints.
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // cors
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        // CSRF 비활성화 (API 서버이므로)
        http.csrf(AbstractHttpConfigurer::disable);
        // HTTP 기본 인증 비활성화
        http.httpBasic(AbstractHttpConfigurer::disable);
        // formLogin 비활성화
        http.formLogin(AbstractHttpConfigurer::disable);

        // 세션 정책을 STATELESS로 설정
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 필터 체인 구성
        AuthenticationManager authenticationManager = authenticationManager(http.getSharedObject(AuthenticationConfiguration.class));
        
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(
                authenticationManager,
                jwtUtility,
                objectMapper,
                redisService,
                cookieDomain,
                accessTokenValidity
        );

        JwtVerificationFilter jwtVerificationFilter = new JwtVerificationFilter(jwtUtility, objectMapper);
        AuthorizationFilter authorizationFilter = new AuthorizationFilter(objectMapper);

        http
            .addFilterAt(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtVerificationFilter, AuthenticationFilter.class)
            .addFilterAfter(authorizationFilter, JwtVerificationFilter.class);

        // 인가 규칙 설정
        http.authorizeHttpRequests((authorize) -> {
            authorize
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN")
                    .anyRequest().authenticated();
        });

        // 사용자 정보 서비스 설정
        http.userDetailsService(principalDetailService);

        return http.build();
    }



    /**
     *  @apiNote ObjectMapper Bean
     *  @return ObjectMapper
     */
    @Bean
    public static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
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

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
