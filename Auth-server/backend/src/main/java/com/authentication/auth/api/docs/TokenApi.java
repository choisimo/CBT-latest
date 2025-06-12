package com.authentication.auth.api.docs;

import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.dto.token.TokenRefreshResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

@Tag(name = "Token Management", description = "JWT 토큰 발급 및 재발급 API")
public interface TokenApi {

    @Operation(summary = "로그인", description = "사용자 ID와 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공", 
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)),
                         headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Set-Cookie", description = "refreshToken=xxxxxx; Path=/; Domain=your-cookie-domain.com; HttpOnly; Secure")
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "로그인 실패 (자격 증명 실패)")
    })
    @PostMapping("/api/auth/login") // Full path as per user_management_api.md
    ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response);

    @Operation(summary = "JWT 토큰 재발급", description = "만료된 Access Token과 유효한 Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공", 
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenRefreshResponse.class)),
                         headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Authorization", description = "Bearer {새로운 액세스 토큰}")
            ),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 없거나 유효하지 않음"),
            @ApiResponse(responseCode = "406", description = "요청이 유효하지 않거나 Redis에 Refresh Token이 없음 (토큰 페이로드 불일치 등)")
    })
    @PostMapping("/api/protected/refresh") // This path is relative to class-level /auth mapping in TokenController
    ResponseEntity<?> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @RequestBody TokenRefreshRequest request) throws IOException;
}
