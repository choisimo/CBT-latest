package com.authentication.auth.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 인증 코드 확인 요청 정보를 담는 불변 레코드
 */
public record EmailCheckRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,
    
    @NotBlank(message = "인증 코드는 필수입니다")
    String code
) {
    /**
     * 유효성 검사
     */
    public boolean isValid() {
        return email != null && !email.isBlank() &&
               code != null && !code.isBlank();
    }
}
