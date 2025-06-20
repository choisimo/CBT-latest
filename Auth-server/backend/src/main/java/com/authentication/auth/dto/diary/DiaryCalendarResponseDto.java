package com.authentication.auth.dto.diary;

import lombok.Builder;

import java.util.List;

@Builder
public record DiaryCalendarResponseDto(
        int year,
        int month,
        List<Integer> daysWithDiary
) {
}
