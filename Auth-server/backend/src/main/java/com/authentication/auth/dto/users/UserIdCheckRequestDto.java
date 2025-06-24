package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 ID 중복 여부 확인 요청 DTO.
 * 프론트엔드는 { "userId": 123 } 형태로 전송한다.
 */
@Schema(description = "사용자 ID 중복 체크 요청 DTO")
public record UserIdCheckRequestDto(
        @Schema(description = "중복 여부를 확인할 사용자 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "사용자 ID는 필수 입력 항목입니다.")
        Integer userId
) {}
