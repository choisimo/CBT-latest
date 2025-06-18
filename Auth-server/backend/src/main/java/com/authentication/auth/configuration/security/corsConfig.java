package com.authentication.auth.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class corsConfig {

    @Value("${site.domain}")
    private String siteDomain;
    @Value("${server.cookie.domain}")
    private String rootDomain;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // For development, allow all origins
        corsConfiguration.setAllowedOrigins(List.of("*")); // More permissive for dev
        corsConfiguration.setAllowedOriginPatterns(List.of("*")); 
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS", "PATCH", "DELETE", "UPDATE"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "provider"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
