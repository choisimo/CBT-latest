package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 닉네임 중복 체크 요청 DTO
 */
public record NicknameCheckRequestDto(
        @Schema(description = "확인할 닉네임", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        String nickname
) {}
