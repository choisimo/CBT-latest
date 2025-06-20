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


/**
 * @Date : 2025-06-20
 * @Detail :  변경 사항  추가 - 
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiClientService {

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "aiService", fallbackMethod = "fallbackAnalyzeDiary")
    @io.github.resilience4j.timelimiter.annotation.TimeLimiter(name = "aiService")
    public Mono<DiaryAnalysisResult> analyzeDiary(String diaryContent) {
        log.info("AI 서버에 다이어리 분석 요청: {}", diaryContent.substring(0, Math.min(50, diaryContent.length())));

        AiChatRequest request = AiChatRequest.createForDiaryAnalysis(diaryContent);

        return webClient.post()
                .uri(aiServerUrl + "/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiChatResponse.class)
                // WebClient's timeout is still useful for the HTTP call itself
                .timeout(Duration.ofSeconds(30)) 
                .map(this::parseAnalysisResult)
                .doOnSuccess(result -> log.info("AI 분석 완료: isNegative={}, hasAlternativeThought={}", 
                    result.isNegative(), result.alternativeThought() != null))
                .doOnError(WebClientResponseException.class, error -> 
                    log.error("AI 분석 WebClientResponseException: Status {}, Body {}", error.getStatusCode(), error.getResponseBodyAsString()))
                .doOnError(e -> !(e instanceof WebClientResponseException), e -> 
                    log.error("AI 분석 실패 (non-WebClientResponseException): {}", e.getMessage()));
                // Note: The .onErrorReturn below might mask the actual error for CircuitBreaker if not handled carefully.
                // The CircuitBreaker will record based on exceptions thrown by the Mono.
                // For reactive flows, ensure errors propagate correctly or use .transform() with Resilience4j Reactor operators for more fine-grained control.
                // For simplicity, keeping onErrorReturn but CB will see the original error if it propagates before this.
                // .onErrorReturn(new DiaryAnalysisResult(false, null, "AI 분석 서비스 오류")); -> This will be handled by fallback
    }

    // Fallback method for analyzeDiary
    @SuppressWarnings("unused") // Parameter diaryContent might not be used in simple fallback
    private Mono<DiaryAnalysisResult> fallbackAnalyzeDiary(String diaryContent, Throwable t) {
        log.warn("AI 분석 fallback 실행 - diaryContent: [{}...], error: {}", 
            diaryContent.substring(0, Math.min(50, diaryContent.length())), t.getMessage());
        return Mono.just(new DiaryAnalysisResult(false, null, "AI 분석 서비스가 현재 불안정합니다. 잠시 후 다시 시도해주세요."));
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