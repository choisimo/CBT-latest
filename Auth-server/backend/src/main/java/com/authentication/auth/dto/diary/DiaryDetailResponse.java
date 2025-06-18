package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import com.authentication.auth.dto.diary.DiaryAnalysisResultDto;
import java.time.LocalDateTime;

public record DiaryDetailResponse(
    Long id,
    Long userId,
    String title,
    String content,
    String alternativeThoughtByAI,
    Boolean isNegative,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    DiaryAnalysisResultDto analysis
) {
    public static DiaryDetailResponse fromEntity(Diary diary, DiaryAnalysisResultDto analysisDto) {
        if (diary == null) {
            return null;
        }
        Long userIdValue = (diary.getUser() != null) ? diary.getUser().getId() : null;

        return new DiaryDetailResponse(
            diary.getId(),
            userIdValue,
            diary.getTitle(),
            diary.getContent(),
            diary.getAlternativeThought(),
            diary.getIsNegative(),
            diary.getCreatedAt(),
            diary.getUpdatedAt(),
            analysisDto
        );
    }

    public static DiaryDetailResponse fromEntity(Diary diary) {
        return fromEntity(diary, null);
    }
}

/**
 * package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import java.time.LocalDateTime;

public record DiaryDetailResponse(Long id, String title, String content, LocalDateTime createdAt, boolean isNegative, String alternativeThought, Long userId) {
    public static DiaryDetailResponse fromEntity(Diary diary) {
        return new DiaryDetailResponse(diary.getId(), diary.getTitle(), diary.getContent(), diary.getCreatedAt(), diary.isNegative(), diary.getAlternativeThought(), diary.getUser().getId());
    }
}
 * 
 */