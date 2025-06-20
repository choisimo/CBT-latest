package com.authentication.auth.dto.diary;

import lombok.Builder;

@Builder
public record PageInfoDto(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
