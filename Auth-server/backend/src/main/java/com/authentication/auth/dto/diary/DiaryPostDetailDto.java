package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) //  null인 필드는 serialize하지 않도록 설정
public record DiaryPostDetailDto(
        String id,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        String title,
        String content,
        boolean aiResponse,
        boolean isNegative,
        String aiAlternativeThoughts,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime updatedAt
) {
    public static DiaryPostDetailDto fromEntity(Diary diary) {
        return DiaryPostDetailDto.builder()
                .id(diary.getId().toString())
                .date(diary.getCreatedAt().toLocalDate())
                .title(diary.getTitle())
                .content(diary.getContent())
                .aiResponse(diary.getAlternativeThought() != null && !diary.getAlternativeThought().isEmpty())
                .isNegative(diary.getIsNegative() != null && diary.getIsNegative())
                .aiAlternativeThoughts(diary.getAlternativeThought())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
