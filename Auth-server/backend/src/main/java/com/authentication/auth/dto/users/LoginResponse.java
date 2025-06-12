package com.authentication.auth.dto.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
    @Schema(description = "로그인 성공 시 발급되는 액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MTYxODU4NzQsImV4cCI6MTcxNjE4NzY3NH0.verylongaccesstokenexample")
    @JsonProperty("access_token") String accessToken
) {}
