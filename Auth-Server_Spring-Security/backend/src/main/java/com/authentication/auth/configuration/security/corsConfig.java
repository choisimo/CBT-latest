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
        corsConfiguration.setAllowedOrigins(List.of(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "https://" + rootDomain,
                        "https://" + siteDomain,
                        "http://192.168.0.44",
                        "http://192.168.0.44:3000"
                ));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS", "PATCH", "DELETE", "UPDATE"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "provider"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
