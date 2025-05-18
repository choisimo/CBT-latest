package com.authentication.auth.dto.users;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 정보를 담는 불변 레코드
 */
public record LoginRequest(
    @NotBlank(message = "사용자 ID는 필수입니다")
    String userId,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    String password
) {
    /**
     * 유효성 검사
     */
    public boolean isValid() {
        return userId != null && !userId.isBlank() &&
               password != null && !password.isBlank();
    }
}
