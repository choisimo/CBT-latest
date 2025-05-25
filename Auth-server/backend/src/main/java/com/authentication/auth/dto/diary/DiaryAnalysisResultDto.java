package com.authentication.auth.dto.diary;

import java.time.LocalDateTime;

// Placeholder DTO - will be fleshed out based on DiaryAnalysisResult entity and API specs.
public record DiaryAnalysisResultDto(
    Long id,
    String emotionDetection,
    String automaticThought,
    String promptForChange,
    String alternativeThought,
    String status,
    LocalDateTime analyzedAt
) {}
