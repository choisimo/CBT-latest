package com.authentication.auth.dto.diary;

public record DiaryAnalysisRequestResponse(
    String message,
    Long diaryId,
    String trackingId // Could be UUID as String
) {}
