package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Schema(description = "사용자 로그인 ID", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "로그인 ID는 필수 입력 항목입니다.")
    String loginId,

    @Schema(description = "사용자 비밀번호", example = "P@sswOrd!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    String password
) {}
