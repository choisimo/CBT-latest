package com.authentication.auth.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 요청 정보를 담는 불변 레코드
 */
public record EmailRequest(
    @Schema(description = "인증 코드 또는 기타 이메일 발송 대상 이메일 주소", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email
) {
    /**
     * 유효성 검사
     */
    public boolean isValid() {
        return email != null && !email.isBlank();
    }
}
