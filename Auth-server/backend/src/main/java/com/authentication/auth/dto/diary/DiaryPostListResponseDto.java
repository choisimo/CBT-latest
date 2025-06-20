package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DiaryPostListResponseDto(
        List<DiaryPostItemDto> data,
        PageInfoDto pageInfo
) {
    public static DiaryPostListResponseDtoBuilder builder() {
        return new DiaryPostListResponseDtoBuilder();
    }
}
