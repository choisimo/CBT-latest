package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import java.time.LocalDateTime;

public record DiaryResponse(
    Long id,
    Long userId,
    String title,
    String content,
    String alternativeThought,
    Boolean isNegative,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static DiaryResponse fromEntity(Diary diary) {
        if (diary == null) {
            return null;
        }
        Long userIdValue = (diary.getUser() != null) ? diary.getUser().getId() : null;
        return new DiaryResponse(
            diary.getId(),
            userIdValue,
            diary.getTitle(),
            diary.getContent(),
            diary.getAlternativeThought(),
            diary.getIsNegative(),
            diary.getCreatedAt(),
            diary.getUpdatedAt()
        );
    }
}

/**
 * package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import java.time.LocalDateTime;

public record DiaryResponse(Long id, String title, String content, LocalDateTime createdAt, boolean isNegative, String alternativeThought) {
    public static DiaryResponse fromEntity(Diary diary) {
        return new DiaryResponse(diary.getId(), diary.getTitle(), diary.getContent(), diary.getCreatedAt(), diary.isNegative(), diary.getAlternativeThought());
    {}
 * 
*/