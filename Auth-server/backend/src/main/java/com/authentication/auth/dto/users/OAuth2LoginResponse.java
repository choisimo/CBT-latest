package com.authentication.auth.dto.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OAuth2 tempCode 응답 후 서버의 토큰 발급 방식의 전달 사용함
 */
@Schema(description = "OAuth2 로그인 응답 DTO")
public record OAuth2LoginResponse(
    @Schema(description = "OAuth2 로그인 성공 시 발급되는 액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJvYXV0aFVzZXIxMjMiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MTYxODU4NzQsImV4cCI6MTcxNjE4NzY3NH0.oauthaccesstokenexample")
    @JsonProperty("access_token") String accessToken,
    @Schema(description = "OAuth2 로그인 성공 시 발급되는 리프레시 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJvYXV0aFVzZXIxMjMiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MTYxODU4NzQsImV4cCI6MTcxNjE4NzY3NH0.oauthrefreshtokenexample")
    @JsonProperty("refresh_token") String refreshToken,
    @Schema(description = "사용자 프로필 정보", example = "{\"userName\":\"oauthUser\",\"email\":\"oauth@example.com\",\"role\":\"USER\",\"createdAt\":\"2023-01-01T12:00:00\",\"lastLogin\":\"2023-01-10T15:30:00\"}")
    UserProfileResponse userProfile
) {}
