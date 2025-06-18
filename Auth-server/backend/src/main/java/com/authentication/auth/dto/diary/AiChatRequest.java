package com.authentication.auth.dto.diary;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record AiChatRequest(
    String message,
    @JsonProperty("conversation_history")
    List<Map<String, String>> conversationHistory,
    @JsonProperty("system_prompt")
    String systemPrompt,
    String model,
    Double temperature,
    @JsonProperty("max_tokens")
    Integer maxTokens,
    Boolean stream
) {
    public static AiChatRequest createForDiaryAnalysis(String diaryContent) {
        return new AiChatRequest(
            diaryContent,
            List.of(),
            "당신은 CBT(인지행동치료) 전문가입니다. 사용자의 일기를 분석하여 부정적인 감정이나 생각이 있는지 판단하고, 대안적 사고를 제안해주세요. 응답은 JSON 형태로 다음과 같이 해주세요: {\"isNegative\": boolean, \"alternativeThought\": \"대안적 사고 내용\"}",
            "gpt-3.5-turbo",
            0.7,
            1000,
            false
        );
    }
} 