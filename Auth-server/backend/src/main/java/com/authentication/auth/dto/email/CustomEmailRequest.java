package com.authentication.auth.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 커스텀 이메일 요청 정보를 담는 불변 레코드
 */
public record CustomEmailRequest(
    @Schema(description = "수신자 이메일 주소", example = "recipient@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,
    
    @Schema(description = "이메일 제목", example = "중요 공지사항입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다")
    String title,
    
    @Schema(description = "이메일 본문 내용", example = "안녕하세요, 서비스 점검 안내드립니다.", requiredMode = Schema.RequiredMode.REQUIRED)
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
