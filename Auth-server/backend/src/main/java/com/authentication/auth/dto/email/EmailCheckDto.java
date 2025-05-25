package com.authentication.auth.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCheckDto(
    @Schema(description = "인증 코드를 확인할 이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 주소를 입력해주세요.")
    String email,

    @Schema(description = "수신된 이메일 인증 코드", example = "A1B2C3D4", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "인증 코드는 필수입니다.")
    String code
) {}
