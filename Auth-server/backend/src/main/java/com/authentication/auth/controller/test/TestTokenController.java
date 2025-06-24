package com.authentication.auth.controller.test;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.test.TestTokenRequest;
import com.authentication.auth.dto.token.TokenDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test")
@Profile({"dev", "test"}) // Activate only for dev and test profiles
public class TestTokenController {

    private final JwtUtility jwtUtility;

    @Autowired
    public TestTokenController(JwtUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    @PostMapping("/token")
    public ResponseEntity<?> generateTestToken(@RequestBody TestTokenRequest tokenRequest) {
        if (tokenRequest.getUserId() == null || tokenRequest.getRoles() == null || tokenRequest.getRoles().isEmpty()) {
            return ResponseEntity.badRequest().body("userId and roles are required.");
        }

        List<SimpleGrantedAuthority> authorities = tokenRequest.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // JwtUtility.buildToken expects userId as String and a collection of GrantedAuthority
        // It returns a TokenDto containing both access and refresh tokens.
        // We'll return the whole DTO, or just the access token if preferred.
        TokenDto tokenDto = jwtUtility.buildToken(tokenRequest.getUserId(), authorities);

        // For now, returning only the access token as a string for simplicity,
        // as the primary goal is API testing with an access token.
        // Could also return the full TokenDto as JSON: ResponseEntity.ok(tokenDto)
        return ResponseEntity.ok(tokenDto.accessToken());
    }
}
