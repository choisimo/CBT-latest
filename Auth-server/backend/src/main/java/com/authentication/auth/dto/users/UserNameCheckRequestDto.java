package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자명 중복 체크 요청 DTO")
public record UserNameCheckRequestDto(
    @Schema(description = "확인할 사용자명 (로그인 ID)", example = "newUser123", requiredMode = Schema.RequiredMode.REQUIRED)
    String userName
) {}
