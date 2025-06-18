package com.authentication.auth.dto.diary;

import java.time.LocalDateTime;

// Placeholder DTO - will be fleshed out based on DiaryAnalysisResult entity and API specs.
public record DiaryAnalysisResultDto(
    Long id,
    String emotionDetection,
    String automaticThought,
    String promptForChange,
    /** Represents the AI-generated alternative thought. Note: If this DTO is part of a larger response that also includes the original Diary's alternativeThought (e.g., within DiaryDetailResponse), this field might be redundant or represent the AI's specific output before it's persisted to the Diary. */
    String alternativeThought,
    String status,
    LocalDateTime analyzedAt
) {}
