package com.authentication.auth.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 요청 정보를 담는 불변 레코드
 */
public record EmailRequest(
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
