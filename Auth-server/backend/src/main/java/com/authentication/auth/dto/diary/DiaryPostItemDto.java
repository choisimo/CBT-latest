package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record DiaryPostItemDto(
        String id,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        String title,
        String contentSnippet,
        boolean aiResponse,
        boolean isNegative,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime createdAt
) {
    public static DiaryPostItemDto fromEntity(Diary diary) {
        String snippet = diary.getContent();
        if (snippet != null && snippet.length() > 100) {
            snippet = snippet.substring(0, 100);
        }

        return DiaryPostItemDto.builder()
                .id(diary.getId().toString())
                .date(diary.getCreatedAt().toLocalDate())
                .title(diary.getTitle())
                .contentSnippet(snippet)
                .aiResponse(diary.getAlternativeThought() != null && !diary.getAlternativeThought().isEmpty())
                .isNegative(diary.getIsNegative() != null && diary.getIsNegative())
                .createdAt(diary.getCreatedAt())
                .build();
    }
}
