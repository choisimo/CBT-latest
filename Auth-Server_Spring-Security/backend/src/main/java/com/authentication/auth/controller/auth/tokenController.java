package com.career_block.auth.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authentication.auth.DTO.token.tokenRefreshRequest;
import com.authentication.auth.configuration.token.jwtUtility;
import com.authentication.auth.service.redis.redisService;
import com.authentication.auth.service.token.tokenService;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/auth")
public class tokenController {

    private final jwtUtility jwtUtility;
    private final redisService redisService;
    private final tokenService  tokenService;

    public tokenController(jwtUtility jwtUtility, redisService redisService, tokenService tokenService){
        this.jwtUtility = jwtUtility;
        this.redisService = redisService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "Refresh JWT Token", description = "Refreshes the expired JWT token and returns a new token if valid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully, new token returned."),
            @ApiResponse(responseCode = "406", description = "Not acceptable, invalid token or no refresh token in Redis."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, refresh token not found in cookies."),
    })
    @PostMapping("/api/protected/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @RequestBody tokenRefreshRequest request) throws IOException {

        if (request == null || request.getExpiredToken() == null || request.getProvider() == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }

        return tokenService.refreshToken(httpRequest, httpResponse, request);
    }
}


