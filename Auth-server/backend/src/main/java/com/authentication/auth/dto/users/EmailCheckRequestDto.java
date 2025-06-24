package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 중복 여부 확인 요청 DTO.
 * 프론트엔드는 { "email": "example@example.com" } 형태로 전송합니다.
 */
@Schema(description = "이메일 중복 체크 요청 DTO")
public record EmailCheckRequestDto(
        @Schema(description = "중복 여부를 확인할 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {
}
