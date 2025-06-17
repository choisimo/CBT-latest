package com.authentication.auth.diary.dto;

import com.authentication.auth.domain.Diary;

import java.time.LocalDateTime;

/**
 * Response DTO for Diary entity.
 */
public record DiaryResponseDto(
        Long diaryId,
        String title,
        String content,
        LocalDateTime createdAt,
        UserSummaryDto author
) {

    public static DiaryResponseDto from(Diary diary) {
        return new DiaryResponseDto(
                diary.getId(),
                diary.getTitle(),
                diary.getContent(),
                diary.getCreatedAt(),
                UserSummaryDto.from(diary.getUser())
        );
    }
}
