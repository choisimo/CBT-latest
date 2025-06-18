package com.authentication.auth.dto.token;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenRefreshRequest(
    @Schema(description = "만료된 Access Token", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MTYxODQwNzQsImV4cCI6MTcxNjE4NTg3NH0.abcdef123456", requiredMode = Schema.RequiredMode.REQUIRED)
    String expiredToken,

    @Schema(description = "토큰 발급자 (예: server, google)", example = "server", requiredMode = Schema.RequiredMode.REQUIRED)
    String provider,

    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MTYxODQwNzQsImV4cCI6MTcxNjE4NTg3NH0.abcdef123456", requiredMode = Schema.RequiredMode.REQUIRED)
    String refreshToken
) {}
