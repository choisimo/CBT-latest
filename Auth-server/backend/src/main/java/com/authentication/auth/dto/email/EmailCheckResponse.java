package com.authentication.auth.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailCheckResponse(
    @Schema(description = "이메일 코드 확인 결과 메시지", example = "email code is valid")
    String message
) {}
