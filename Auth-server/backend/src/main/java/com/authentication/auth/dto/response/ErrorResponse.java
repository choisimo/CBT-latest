package com.authentication.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Schema(description = "API 에러 응답 DTO")
public record ErrorResponse(
    @Schema(description = "에러 발생 타임스탬프 (UTC)", example = "2025-05-25T10:30:00.123Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    LocalDateTime timestamp,

    @Schema(description = "HTTP 상태 코드", example = "404")
    int status,

    @Schema(description = "HTTP 상태 메시지", example = "Not Found")
    String error,

    @Schema(description = "에러 상세 메시지", example = "요청한 리소스를 찾을 수 없습니다.")
    String message,

    @Schema(description = "에러가 발생한 요청 경로", example = "/api/users/999")
    String path
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now(ZoneOffset.UTC), status, error, message, path);
    }
}
