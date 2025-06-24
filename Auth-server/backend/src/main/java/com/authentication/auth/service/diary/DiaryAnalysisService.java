package com.authentication.auth.service.diary;

import com.authentication.auth.domain.AIResponse;
import com.authentication.auth.dto.diary.DiaryRequestDto;
import com.authentication.auth.dto.AIResponseDto;
import com.authentication.auth.dto.DiaryAnalysisRequestDto;
import com.authentication.auth.repository.AIResponseRepository;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;


import jakarta.annotation.PostConstruct;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryAnalysisService {

    private final AIResponseRepository aiResponseRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @PostConstruct
    private void configureObjectMapper() {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        log.info("ObjectMapper configured to allow unquoted control characters");
    }

    @Async
    public void requestDiaryAnalysis(DiaryRequestDto diaryRequest) {
        log.info("비동기 일기 분석 시작 - 사용자 ID: {}", diaryRequest.getUserId());
        try {
            analyzeAndSaveDiary(diaryRequest);
            log.info("비동기 일기 분석 완료 - 사용자 ID: {}", diaryRequest.getUserId());
        } catch (Exception e) {
            log.error("비동기 일기 분석 중 오류 발생 - 사용자 ID: {}: {}", diaryRequest.getUserId(), e.getMessage(), e);
        }
    }

    public AIResponseDto analyzeAndSaveDiary(DiaryRequestDto diaryRequest) {
        try {
            String aiResponse = callAIServer(diaryRequest);
            AIResponse parsedResponse = parseAIResponse(aiResponse, diaryRequest);
            AIResponse savedResponse = aiResponseRepository.save(parsedResponse);
            return convertToDto(savedResponse);
        } catch (Exception e) {
            log.error("일기 분석 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("일기 분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String callAIServer(DiaryRequestDto diaryRequest) {
        WebClient webClient = webClientBuilder
            .baseUrl(aiServerUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        DiaryAnalysisRequestDto analysisRequest = DiaryAnalysisRequestDto.builder()
            .title(diaryRequest.getTitle())
            .content(diaryRequest.getContent())
            .userId(diaryRequest.getUserId())
            .build();

        try {
            // AI 서버의 응답을 DTO가 아닌 String으로 직접 받습니다.
            String rawResponse = webClient
                .post()
                .uri("/diary/analyze")
                .bodyValue(analysisRequest)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // AI 서버의 원시 응답을 로그로 남겨 디버깅을 용이하게 합니다.
            log.info("AI 서버 원시 응답: {}", rawResponse);

            // 응답이 비어있는지 확인합니다.
            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 비어있는 응답을 받았습니다.");
                throw new RuntimeException("AI 서버로부터 비어있는 응답을 받았습니다.");
            }

            // 수동으로 JSON을 파싱하여 'response' 필드를 추출합니다.
            JsonNode rootNode = objectMapper.readTree(rawResponse);
            JsonNode responseNode = rootNode.get("response");

            if (responseNode == null || !responseNode.isTextual()) {
                log.error("AI 서버 응답에 'response' 필드가 없거나 텍스트가 아닙니다. 응답: {}", rawResponse);
                throw new RuntimeException("AI 서버로부터 유효하지 않은 형식의 응답을 받았습니다.");
            }

            return responseNode.asText();

        } catch (WebClientResponseException e) {
            log.error("AI 서버 호출 실패: Status {}, Body {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI 서버 호출에 실패했습니다.");
        } catch (JsonProcessingException e) {
            log.error("AI 서버 응답 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 응답을 파싱하는 데 실패했습니다.");
        }
    }

    private AIResponse parseAIResponse(String jsonResponse, DiaryRequestDto diaryRequest) throws JsonProcessingException {
        // ... (rest of the original file content)
        return new AIResponse();
    }

    private AIResponseDto convertToDto(AIResponse aiResponse) {
        // ... (rest of the original file content)
        return new AIResponseDto();
    }
}
