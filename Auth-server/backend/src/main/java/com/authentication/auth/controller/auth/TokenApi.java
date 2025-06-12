package com.authentication.auth.controller.auth;

import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.dto.token.TokenRefreshResponse;
import com.authentication.auth.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;

@Tag(name = "Token Management", description = "JWT 토큰 발급 및 재발급 API")
public interface TokenApi {

    @Operation(summary = "로그인", description = "사용자 ID와 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = LoginResponse.class),
                                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "로그인 성공 응답", value = "{\"access_token\": \"eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MDEyMzQ1NjcsImV4cCI6MTcwMTI0MTc2N30.exampleTokenString\"}")),
                         headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Set-Cookie", description = "refreshToken=xxxxxx; Path=/; Domain=your-cookie-domain.com; HttpOnly; Secure")
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "잘못된 요청 응답", value = "{\"timestamp\": \"2023-10-27T10:05:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Request body is missing or malformed.\", \"path\": \"/api/auth/login\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인 실패 (자격 증명 실패)",
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "로그인 실패 응답", value = "{\"timestamp\": \"2023-10-27T10:06:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Invalid credentials. Please check your user ID and password.\", \"path\": \"/api/auth/login\"}")))
    })
    @PostMapping("/api/auth/login") // Full path as per user_management_api.md
    ResponseEntity<?> login(
            @RequestBody(
                description = "사용자 로그인 정보",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "정상 로그인 예시",
                            summary = "올바른 사용자 ID와 비밀번호로 로그인하는 경우입니다.",
                            value = "{\"userId\": \"testuser1\", \"password\": \"password123!\"}"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "잘못된 비밀번호 예시",
                            summary = "존재하는 사용자 ID에 잘못된 비밀번호를 입력한 경우입니다. (401 Unauthorized 예상)",
                            value = "{\"userId\": \"testuser1\", \"password\": \"wrongPassword!\"}"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "존재하지 않는 사용자 ID 예시",
                            summary = "시스템에 등록되지 않은 사용자 ID로 로그인을 시도하는 경우입니다. (401 Unauthorized 예상)",
                            value = "{\"userId\": \"nonexistentuser\", \"password\": \"anypassword\"}"
                        )
                    }
                )
            ) LoginRequest loginRequest, HttpServletResponse response);

    @Operation(summary = "JWT 토큰 재발급", description = "만료된 Access Token과 유효한 Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = TokenRefreshResponse.class),
                                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "재발급 성공 응답", value = "{\"access_token\": \"new_eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzAxMjM0NTY4LCJleHAiOjE3MDEyMzYzNjh9.anotherExampleToken\"}"))),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 없거나 유효하지 않음", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "유효하지 않은 리프레시 토큰", value = "{\"timestamp\": \"2023-10-27T10:20:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Refresh token is missing, expired, or invalid.\", \"path\": \"/auth/api/protected/refresh\"}"))),
            @ApiResponse(responseCode = "406", description = "요청이 유효하지 않거나 Redis에 Refresh Token이 없음", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "리프레시 토큰 없음 또는 잘못된 요청", value = "{\"timestamp\": \"2023-10-27T10:21:00Z\", \"status\": 406, \"error\": \"Not Acceptable\", \"message\": \"The request is invalid or the refresh token for provider 'server' was not found.\", \"path\": \"/auth/api/protected/refresh\"}")))
    })
    @PostMapping("/api/protected/refresh") // This path is relative to class-level /auth mapping in TokenController
    ResponseEntity<?> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @RequestBody TokenRefreshRequest request) throws IOException;
}
