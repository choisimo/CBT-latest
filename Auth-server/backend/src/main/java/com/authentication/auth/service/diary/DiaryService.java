package com.authentication.auth.service;

import com.authentication.auth.domain.AIResponse;
import com.authentication.auth.dto.*;
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
public class DiaryService {

    private final AIResponseRepository aiResponseRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    /**
     * ObjectMapper 설정을 조정하여 제어 문자들을 허용
     */
    @PostConstruct
    private void configureObjectMapper() {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        log.info("ObjectMapper configured to allow unquoted control characters");
    }

    /**
     * 일기 분석 및 AI 응답 생성
     */
    public AIResponseDto analyzeAndSaveDiary(DiaryRequestDto diaryRequest) {
        try {
            // 1. AI 서버에 요청 보내기
            String aiResponse = callAIServer(diaryRequest);
            
            // 2. AI 응답 파싱
            AIResponse parsedResponse = parseAIResponse(aiResponse, diaryRequest);
            
            // 3. MongoDB에 저장
            AIResponse savedResponse = aiResponseRepository.save(parsedResponse);
            
            // 4. DTO로 변환하여 반환
            return convertToDto(savedResponse);
            
        } catch (Exception e) {
            log.error("일기 분석 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("일기 분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * AI 서버 호출
     */
    private String callAIServer(DiaryRequestDto diaryRequest) {
        WebClient webClient = webClientBuilder
            .baseUrl(aiServerUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        // 일기 분석 요청 구성
        DiaryAnalysisRequestDto analysisRequest = DiaryAnalysisRequestDto.builder()
            .title(diaryRequest.getTitle())
            .content(diaryRequest.getContent())
            .userId(diaryRequest.getUserId())
            .build();

        try {
            ChatResponseDto response = webClient
                .post()
                .uri("/diary/analyze")
                .bodyValue(analysisRequest)
                .retrieve()
                .bodyToMono(ChatResponseDto.class)
                .block();

            if (response == null || response.getResponse() == null) {
                throw new RuntimeException("AI 서버로부터 응답을 받지 못했습니다.");
            }

            return response.getResponse();

        } catch (WebClientResponseException e) {
            log.error("AI 서버 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 호출에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * AI 응답 파싱
     */
    private AIResponse parseAIResponse(String aiResponseStr, DiaryRequestDto diaryRequest) throws JsonProcessingException {
        log.debug("원본 AI 응답: {}", aiResponseStr);
        
        // 임시 ObjectMapper 생성 (제어 문자 허용 설정)
        ObjectMapper tempMapper = new ObjectMapper();
        tempMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        tempMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        JsonNode jsonNode;
        try {
            // 먼저 원본 JSON 파싱 시도
            jsonNode = tempMapper.readTree(aiResponseStr);
        } catch (JsonProcessingException e) {
            log.warn("원본 JSON 파싱 실패, 이스케이프 처리 후 재시도: {}", e.getMessage());
            // 파싱 실패시 이스케이프 처리 후 재시도
            String processedResponse = escapeJsonString(aiResponseStr);
            log.debug("전처리된 AI 응답: {}", processedResponse);
            jsonNode = tempMapper.readTree(processedResponse);
        }
        JsonNode aiResponseNode = jsonNode.get("aiResponse");

        if (aiResponseNode == null) {
            throw new RuntimeException("AI 응답 형식이 올바르지 않습니다.");
        }

        // 감정 정보 파싱
        List<AIResponse.Emotion> emotions = new ArrayList<>();
        JsonNode emotionsNode = aiResponseNode.get("emotions");
        if (emotionsNode != null && emotionsNode.isArray()) {
            for (JsonNode emotionNode : emotionsNode) {
                AIResponse.Emotion emotion = AIResponse.Emotion.builder()
                    .category(emotionNode.get("category").asText())
                    .intensity(emotionNode.get("intensity").asInt())
                    .build();
                emotions.add(emotion);
            }
        }

        // AIResponse 엔티티 생성
        LocalDateTime now = LocalDateTime.now();
        
        return AIResponse.builder()
            .userId(diaryRequest.getUserId())
            .diaryTitle(diaryRequest.getTitle())
            .diaryContent(diaryRequest.getContent())
            .emotions(emotions)
            .summary(aiResponseNode.get("summary").asText())
            .coaching(aiResponseNode.get("coaching").asText())
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    /**
     * JSON 문자열 이스케이프 처리 (JSON 값 내의 특수 문자만 처리)
     */
    private String escapeJsonString(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < jsonStr.length(); i++) {
            char c = jsonStr.charAt(i);
            
            if (escapeNext) {
                // 이미 이스케이프된 문자는 그대로 유지
                sb.append('\\').append(c);
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                continue;
            }
            
            if (c == '"' && !escapeNext) {
                inString = !inString;
                sb.append(c);
                continue;
            }
            
            if (inString) {
                // 문자열 값 내부에서만 특수 문자 이스케이프 처리
                switch (c) {
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    case '\b':
                        sb.append("\\b");
                        break;
                    case '\f':
                        sb.append("\\f");
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }

    /**
     * 모든 AI 응답 조회 (특정 사용자)
     */
    public List<AIResponseDto> getAllAIResponses(String userId) {
        List<AIResponse> responses = aiResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * 기간별 AI 응답 조회
     */
    public List<AIResponseDto> getAIResponsesByDateRange(String userId, String startDate, String endDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
            
            List<AIResponse> responses = aiResponseRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
            return responses.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("기간별 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("날짜 형식이 올바르지 않습니다. (yyyy-MM-dd 형식을 사용해주세요)");
        }
    }

    /**
     * AI 응답 단건 조회
     */
    public Optional<AIResponseDto> getAIResponseById(String id) {
        return aiResponseRepository.findById(id)
            .map(this::convertToDto);
    }

    /**
     * AI 응답 삭제
     */
    public void deleteAIResponse(String id) {
        aiResponseRepository.deleteById(id);
    }

    /**
     * AI 응답 수정
     */
    public AIResponseDto updateAIResponse(String id, AIResponseDto updateDto) {
        AIResponse existingResponse = aiResponseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("해당 AI 응답을 찾을 수 없습니다."));

        // 수정 가능한 필드들 업데이트
        existingResponse.setDiaryTitle(updateDto.getDiaryTitle());
        existingResponse.setDiaryContent(updateDto.getDiaryContent());
        existingResponse.setUpdatedAt(LocalDateTime.now());

        AIResponse savedResponse = aiResponseRepository.save(existingResponse);
        return convertToDto(savedResponse);
    }

    /**
     * Entity to DTO 변환
     */
    private AIResponseDto convertToDto(AIResponse aiResponse) {
        List<AIResponseDto.EmotionDto> emotionDtos = aiResponse.getEmotions().stream()
            .map(emotion -> AIResponseDto.EmotionDto.builder()
                .category(emotion.getCategory())
                .intensity(emotion.getIntensity())
                .build())
            .collect(Collectors.toList());

        return AIResponseDto.builder()
            .id(aiResponse.getId())
            .userId(aiResponse.getUserId())
            .diaryTitle(aiResponse.getDiaryTitle())
            .diaryContent(aiResponse.getDiaryContent())
            .emotions(emotionDtos)
            .summary(aiResponse.getSummary())
            .coaching(aiResponse.getCoaching())
            .createdAt(aiResponse.getCreatedAt())
            .updatedAt(aiResponse.getUpdatedAt())
            .build();
    }
} 