package com.authentication.auth.dto.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
    @Schema(description = "로그인 성공 시 발급되는 액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MTYxODU4NzQsImV4cCI6MTcxNjE4NzY3NH0.verylongaccesstokenexample")
    @JsonProperty("access_token") String accessToken,
    @Schema(description = "로그인한 사용자 정보")
    UserInfo user
) {
    /**
     * 사용자 정보 DTO
     */
    public record UserInfo(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        @Schema(description = "사용자명", example = "testuser")
        String nickname,
        @Schema(description = "이메일", example = "user@example.com")
        String email
    ) {}
}
