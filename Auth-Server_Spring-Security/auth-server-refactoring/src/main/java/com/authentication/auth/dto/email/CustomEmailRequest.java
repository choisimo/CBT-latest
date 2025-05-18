package com.authentication.auth.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 커스텀 이메일 요청 정보를 담는 불변 레코드
 */
public record CustomEmailRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,
    
    @NotBlank(message = "제목은 필수입니다")
    String title,
    
    @NotBlank(message = "내용은 필수입니다")
    String content
) {
    /**
     * 유효성 검사
     */
    public boolean isValid() {
        return email != null && !email.isBlank() &&
               title != null && !title.isBlank() &&
               content != null && !content.isBlank();
    }
}
