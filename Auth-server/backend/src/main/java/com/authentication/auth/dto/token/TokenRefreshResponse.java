package com.authentication.auth.dto.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record TokenRefreshResponse(
    @Schema(description = "새로 발급된 액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MTYxODU4NzQsImV4cCI6MTcxNjE4NzY3NH0.brandnewaccesstokenexample")
    @JsonProperty("access_token") String accessToken
) {}
