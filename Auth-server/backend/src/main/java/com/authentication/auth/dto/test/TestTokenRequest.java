package com.authentication.auth.dto.test;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class TestTokenRequest {
    private String userId; // Changed from Long to String to match JwtUtility
    private String username; // username is not directly used by JwtUtility for claims, but can be kept for request structure
    private List<String> roles;
}
