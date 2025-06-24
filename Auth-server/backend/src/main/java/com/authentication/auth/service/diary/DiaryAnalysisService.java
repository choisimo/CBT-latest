package com.authentication.auth.service.diary;

import com.authentication.auth.domain.AIResponse;
import com.authentication.auth.domain.Diary;
import com.authentication.auth.dto.ai.AIAnalysisResponse;
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
import com.authentication.auth.service.sse.SseService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryAnalysisService {

    private final AIResponseRepository aiResponseRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final SseService sseService;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @PostConstruct
    private void configureObjectMapper() {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        log.info("ObjectMapper configured to allow unquoted control characters");
    }

    @Async
    public void requestDiaryAnalysis(Diary diary) {
        log.info("비동기 일기 분석 시작 - 사용자 ID: {}", diary.getUser().getLoginId());
        try {
            analyzeAndSaveDiary(diary);
            log.info("비동기 일기 분석 완료 - 사용자 ID: {}", diary.getUser().getLoginId());
        } catch (Exception e) {
            log.error("비동기 일기 분석 중 오류 발생 - 사용자 ID: {}: {}", diary.getUser().getLoginId(), e.getMessage(), e);
        }
    }

    public AIResponseDto analyzeAndSaveDiary(Diary diary) {
        String userId = diary.getUser().getLoginId();
        
        try {
            // 1. 먼저 PENDING 상태의 AIResponse 레코드를 생성하고 저장
            AIResponse pendingResponse = AIResponse.builder()
                    .diaryId(diary.getId())
                    .userId(userId)
                    .diaryTitle(diary.getTitle())
                    .diaryContent(diary.getContent())
                    .status(AIResponse.AnalysisStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            AIResponse savedPendingResponse = aiResponseRepository.save(pendingResponse);
            log.info("AI 분석 요청 레코드 생성됨. ID: {}, Status: PENDING", savedPendingResponse.getId());

            // SSE로 PENDING 상태 알림
            AIResponseDto pendingDto = convertToDto(savedPendingResponse);
            sseService.sendEventToUser(userId, "analysis_status", pendingDto);

            // 2. AI 서버를 호출하여 구조화된 응답(DTO)을 받습니다.
            savedPendingResponse.setStatus(AIResponse.AnalysisStatus.PROCESSING);
            savedPendingResponse.setUpdatedAt(LocalDateTime.now());
            aiResponseRepository.save(savedPendingResponse);
            log.info("AI 분석 시작. ID: {}, Status: PROCESSING", savedPendingResponse.getId());

            // SSE로 PROCESSING 상태 알림
            AIResponseDto processingDto = convertToDto(savedPendingResponse);
            sseService.sendEventToUser(userId, "analysis_status", processingDto);

            AIAnalysisResponse aiAnalysisResponse = callAIServer(diary);

            // 3. 받은 응답을 바탕으로 AIResponse 엔티티를 완성하고 저장합니다.
            AIResponse.Emotion emotion = AIResponse.Emotion.builder()
                    .category(aiAnalysisResponse.getEmotion())
                    .build();

            savedPendingResponse.setEmotions(java.util.List.of(emotion));
            savedPendingResponse.setCoaching(aiAnalysisResponse.getSolution());
            savedPendingResponse.setStatus(AIResponse.AnalysisStatus.COMPLETED);
            savedPendingResponse.setUpdatedAt(LocalDateTime.now());

            AIResponse completedResponse = aiResponseRepository.save(savedPendingResponse);
            log.info("AI 분석 완료 및 저장. ID: {}, Status: COMPLETED", completedResponse.getId());

            // 4. 저장된 엔티티를 DTO로 변환하여 반환합니다.
            AIResponseDto completedDto = convertToDto(completedResponse);
            
            // SSE로 완료 상태 및 최종 결과 알림
            sseService.sendEventToUser(userId, "analysis_complete", completedDto);
            
            return completedDto;

        } catch (Exception e) {
            log.error("일기 분석 및 저장 중 오류 발생: {}", e.getMessage(), e);
            
            // 실패 상태로 업데이트 시도
            try {
                Optional<AIResponse> existingResponse = aiResponseRepository.findByDiaryId(diary.getId());
                if (existingResponse.isPresent()) {
                    AIResponse failedResponse = existingResponse.get();
                    failedResponse.setStatus(AIResponse.AnalysisStatus.FAILED);
                    failedResponse.setErrorMessage(e.getMessage());
                    failedResponse.setUpdatedAt(LocalDateTime.now());
                    aiResponseRepository.save(failedResponse);
                    log.info("AI 분석 실패 상태로 업데이트됨. ID: {}, Status: FAILED", failedResponse.getId());
                    
                    // SSE로 실패 상태 알림
                    AIResponseDto failedDto = convertToDto(failedResponse);
                    sseService.sendEventToUser(userId, "analysis_failed", failedDto);
                }
            } catch (Exception updateException) {
                log.error("실패 상태 업데이트 중 추가 오류 발생: {}", updateException.getMessage(), updateException);
            }
            
            throw new RuntimeException("일기 분석 및 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private AIAnalysisResponse callAIServer(Diary diary) {
        WebClient webClient = webClientBuilder
                .baseUrl(aiServerUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // AI 서버에는 일기 내용만 보내도록 단순화합니다.
        DiaryAnalysisRequestDto analysisRequest = DiaryAnalysisRequestDto.builder()
                .text(diary.getTitle() + "\n\n" + diary.getContent())
                .build();

        try {
            // AI 서버의 응답을 AIAnalysisResponse DTO로 직접 변환합니다.
            AIAnalysisResponse aiResponse = webClient
                    .post()
                    .uri("/diary/analyze")
                    .bodyValue(analysisRequest)
                    .retrieve()
                    .bodyToMono(AIAnalysisResponse.class)
                    .block();

            if (aiResponse == null || aiResponse.getEmotion() == null || aiResponse.getSolution() == null) {
                log.error("AI 서버로부터 유효하지 않은 응답을 받았습니다. 응답: {}", aiResponse);
                throw new RuntimeException("AI 서버로부터 유효하지 않은 형식의 응답을 받았습니다.");
            }

            log.info("AI 서버 구조화 응답: Emotion='{}', Solution='{}'", aiResponse.getEmotion(), aiResponse.getSolution());
            return aiResponse;

        } catch (WebClientResponseException e) {
            log.error("AI 서버 호출 실패: Status {}, Body {}", e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("AI 서버 호출에 실패했습니다.");
        } catch (Exception e) {
            log.error("AI 서버 호출 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("AI 서버와 통신 중 오류가 발생했습니다.");
        }
    }



    private AIResponseDto convertToDto(AIResponse aiResponse) {
        if (aiResponse == null) {
            return null;
        }

        AIResponseDto.AIResponseDtoBuilder builder = AIResponseDto.builder()
                .id(aiResponse.getId())
                .userId(aiResponse.getUserId())
                .diaryTitle(aiResponse.getDiaryTitle())
                .diaryContent(aiResponse.getDiaryContent())
                .summary(aiResponse.getSummary())
                .coaching(aiResponse.getCoaching())
                .createdAt(aiResponse.getCreatedAt())
                .updatedAt(aiResponse.getUpdatedAt())
                .status(aiResponse.getStatus() != null ? aiResponse.getStatus().name() : "UNKNOWN")
                .errorMessage(aiResponse.getErrorMessage());

        // 감정 데이터가 있는 경우 변환
        if (aiResponse.getEmotions() != null && !aiResponse.getEmotions().isEmpty()) {
            List<AIResponseDto.EmotionDto> emotionDtos = aiResponse.getEmotions().stream()
                    .map(emotion -> AIResponseDto.EmotionDto.builder()
                            .category(emotion.getCategory())
                            .intensity(emotion.getIntensity())
                            .build())
                    .collect(Collectors.toList());
            builder.emotions(emotionDtos);
        }

        return builder.build();
    }
}
