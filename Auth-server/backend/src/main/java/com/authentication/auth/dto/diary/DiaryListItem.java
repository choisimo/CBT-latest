package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import java.time.LocalDateTime;

public record DiaryListItem(
    Long id,
    String title,
    LocalDateTime createdAt,
    String emotionStatus
) {
    public static DiaryListItem fromEntity(Diary diary) {
        if (diary == null) {
            return null;
        }
        String status = "NEUTRAL"; // Default status if isNegative is null
        if (diary.getIsNegative() != null) {
            status = diary.getIsNegative() ? "NEGATIVE" : "POSITIVE";
        }
        // Future enhancement: If a more detailed emotion analysis (e.g., from DiaryAnalysisResult)
        // is readily available or linked here, it could be used to set a more granular status.

        return new DiaryListItem(
            diary.getId(),
            diary.getTitle(),
            diary.getCreatedAt(),
            status
        );
    }
}

/**
 * package com.authentication.auth.dto.diary;

import com.authentication.auth.domain.Diary;
import java.time.LocalDateTime;

public record DiaryListItem(Long id, String title, LocalDateTime createdAt, boolean isNegative) {
    public static DiaryListItem fromEntity(Diary diary) {
        return new DiaryListItem(diary.getId(), diary.getTitle(), diary.getCreatedAt(), diary.isNegative());
    }
}
 * 
 */