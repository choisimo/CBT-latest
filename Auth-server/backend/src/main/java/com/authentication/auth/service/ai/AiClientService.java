package com.authentication.auth.service.ai;

import com.authentication.auth.dto.diary.AiChatRequest;
import com.authentication.auth.dto.diary.AiChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiClientService {

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public Mono<DiaryAnalysisResult> analyzeDiary(String diaryContent) {
        log.info("AI 서버에 다이어리 분석 요청: {}", diaryContent.substring(0, Math.min(50, diaryContent.length())));

        AiChatRequest request = AiChatRequest.createForDiaryAnalysis(diaryContent);

        return webClient.post()
                .uri(aiServerUrl + "/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiChatResponse.class)
                .timeout(Duration.ofSeconds(30))
                .map(this::parseAnalysisResult)
                .doOnSuccess(result -> log.info("AI 분석 완료: isNegative={}, hasAlternativeThought={}", 
                    result.isNegative(), result.alternativeThought() != null))
                .doOnError(error -> log.error("AI 분석 실패: {}", error.getMessage()))
                .onErrorReturn(new DiaryAnalysisResult(false, null, "AI 분석 서비스 오류"));
    }

    private DiaryAnalysisResult parseAnalysisResult(AiChatResponse response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response.response());
            
            boolean isNegative = jsonNode.path("isNegative").asBoolean(false);
            String alternativeThought = jsonNode.path("alternativeThought").asText(null);
            
            return new DiaryAnalysisResult(isNegative, alternativeThought, null);
            
        } catch (JsonProcessingException e) {
            log.warn("AI 응답 파싱 실패, 원본 응답을 대안적 사고로 사용: {}", e.getMessage());
            // JSON 파싱 실패 시 원본 응답을 대안적 사고로 사용
            return new DiaryAnalysisResult(false, response.response(), null);
        }
    }

    public record DiaryAnalysisResult(
        boolean isNegative,
        String alternativeThought,
        String error
    ) {}
} 