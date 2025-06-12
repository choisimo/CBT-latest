package com.authentication.auth.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailSendResponse(
    @Schema(description = "이메일 발송 결과 메시지", example = "A temporary code has been sent to your email")
    String message
) {}
