package com.authentication.auth.controller.auth;

import com.authentication.auth.api.docs.TokenApi;
import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.service.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor // Using Lombok for constructor injection
public class TokenController implements TokenApi {

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager; // Injected AuthenticationManager

    // Constructor removed as @RequiredArgsConstructor will generate it.
    // public TokenController(TokenService tokenService){
    // this.tokenService = tokenService;
    // }

    @Override
    @PostMapping("/login") // Ensuring PostMapping is present as per standard practices, overriding from interface
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse) {
        log.info("Login attempt for user: {}", loginRequest.userId());

        // Create authentication token
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.userId(), loginRequest.password());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // If authentication is successful, proceed with post-login actions
        LoginResponse loginResponse = tokenService.postLoginActions(authentication, httpServletResponse);

        return ResponseEntity.ok(loginResponse);
    }

    @Override
    @PostMapping("/refresh") // Ensuring PostMapping is present
    public ResponseEntity<?> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @RequestBody TokenRefreshRequest request) throws IOException {

        if (request == null || request.expiredToken() == null || request.provider() == null) {
            // This basic validation can remain, or be part of the service layer
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Invalid refresh token request payload");
        }

        return tokenService.refreshToken(httpRequest, httpResponse, request);
    }
}
