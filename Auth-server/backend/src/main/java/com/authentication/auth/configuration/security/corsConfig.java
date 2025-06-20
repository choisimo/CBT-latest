package com.authentication.auth.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j; // Added for logging

@Slf4j // Added for logging
@Configuration
public class corsConfig {

    @Value("${site.domain}")
    private String siteDomain; // This seems unused in current CORS logic, but kept for now.
    @Value("${server.cookie.domain}")
    private String rootDomain; // This seems unused in current CORS logic, but kept for now.

    @Value("${cors.allowed-origins.prod}")
    private String prodAllowedOrigins;

    @Value("${cors.allowed-origins.dev}")
    private String devAllowedOrigins;
    
    @Value("${spring.profiles.active:default}") // Get active profile, default to "default" if not set
    private String activeProfile;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        List<String> allowedOriginsConfig;
        if ("dev".equalsIgnoreCase(activeProfile)) {
            allowedOriginsConfig = Arrays.asList(devAllowedOrigins.split("\\s*,\\s*"));
            // For dev, it can be useful to allow all patterns if localhost issues persist with specific ports,
            // but it's better to be explicit. For now, we rely on the list.
            // corsConfiguration.setAllowedOriginPatterns(List.of("http://localhost:[*]", "http://127.0.0.1:[*]"));
        } else {
            // For prod or any other profile, use production origins
            allowedOriginsConfig = Arrays.asList(prodAllowedOrigins.split("\\s*,\\s*"));
        }
        
        log.info("Active Spring profile: {}", activeProfile);
        log.info("Configuring CORS for allowed origins: {}", allowedOriginsConfig);

        corsConfiguration.setAllowedOrigins(allowedOriginsConfig);
        // corsConfiguration.setAllowedOriginPatterns(List.of("*")); // REMOVED as per audit
        
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS", "PATCH", "DELETE", "UPDATE"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "provider", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        corsConfiguration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Authorization"));
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L); // Optional: How long the response from a pre-flight request can be cached.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
