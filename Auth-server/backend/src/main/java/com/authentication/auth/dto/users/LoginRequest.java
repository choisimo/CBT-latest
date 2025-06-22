package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Schema(description = "사용자 로그인 ID", example = "user123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String loginId,

    @Schema(description = "사용자 이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,

    @Schema(description = "사용자 비밀번호", example = "P@sswOrd!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    String password
) {
    /**
     * 로그인 식별자(ID 또는 이메일) 반환
     */
    public String identifier() {
        return (loginId != null && !loginId.isBlank()) ? loginId : email;
    }
}
