package com.authentication.auth.dto.smtp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SmtpEmailRequest(
    @Schema(description = "SMTP 테스트 이메일 발송 대상 주소", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email
    @NotBlank
    String email
) {}
