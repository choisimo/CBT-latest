package com.authentication.auth.configuration.security.handler;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.token.PrincipalDetails; // Assuming PrincipalDetails is used for OAuth2 user details
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtility jwtUtility;
    // private final String appScheme = "mycbtapp"; // Could be injected from properties

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        // Here, authentication object should contain details from OAuth2 provider
        // You might need to cast to a specific OAuth2User or PrincipalDetails
        String userId;
        if (authentication.getPrincipal() instanceof PrincipalDetails) {
            userId = ((PrincipalDetails) authentication.getPrincipal()).getUsername();
        } else if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            // Extract a unique identifier from OAuth2User, e.g., email or sub
            userId = ((org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal()).getName(); 
            // This might need adjustment based on how OAuth2User is populated by your CustomOAuth2UserService
            log.warn("OAuth2User principal detected, ensure userId extraction is correct. Extracted: {}", userId);
        } else {
            log.error("Cannot determine user ID from principal of type: {}", authentication.getPrincipal().getClass().getName());
            // Handle error - perhaps redirect to an error page within the app or a generic error
            super.onAuthenticationSuccess(request, response, authentication); // Default behavior
            return;
        }
        
        log.info("OAuth2 Authentication successful for user: {}. Generating app token.", userId);
        
        // Generate your application's JWT token
        // Assuming buildToken takes userId and authorities. Authorities might be mapped from OAuth scopes.
        com.authentication.auth.dto.token.TokenDto appTokenDto = jwtUtility.buildToken(userId, authentication.getAuthorities());
        String appAccessToken = appTokenDto.accessToken();
        // String appRefreshToken = appTokenDto.refreshToken(); // If you also pass refresh token

        String targetUrl = UriComponentsBuilder.fromUriString("mycbtapp://oauth/callback")
                .queryParam("token", appAccessToken)
                // .queryParam("refresh_token", appRefreshToken) // Optionally add refresh token
                .build().toUriString();
        
        log.info("Redirecting to target URL: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
