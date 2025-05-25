package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
    @Schema(description = "사용자 로그인 ID", example = "testuser123", requiredMode = Schema.RequiredMode.REQUIRED)
    String userId,
    @Schema(description = "사용자 비밀번호", example = "P@sswOrd!", requiredMode = Schema.RequiredMode.REQUIRED)
    String password
) {}
