package com.authentication.auth.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자명 중복 체크 요청 DTO - 사용자명은 유니크한 아이디를 의미함, 서버측에서 자동 생성해주는 해시 값") 
public record UserNameCheckRequestDto(
    @Schema(description = "확인할 사용자명 (로그인 ID) - 사용자명은 유니크한 아이디를 의미함", example = "newUser123", requiredMode = Schema.RequiredMode.REQUIRED)
    String userName
) {}
