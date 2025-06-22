package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 ID 중복 여부 확인 요청 DTO.
 * 프론트엔드는 { "loginId" : "some_id" } 형태로 전송한다.
 */
@Schema(description = "로그인 ID 중복 체크 요청 DTO")
public record LoginIdCheckRequestDto(
        @Schema(description = "중복 여부를 확인할 로그인 ID", example = "newUser123", requiredMode = Schema.RequiredMode.REQUIRED)
        String loginId
) {
}
