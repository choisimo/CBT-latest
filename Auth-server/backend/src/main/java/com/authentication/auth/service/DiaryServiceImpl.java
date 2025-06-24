package com.authentication.auth.service;

import com.authentication.auth.domain.AIResponse;
import com.authentication.auth.dto.AIResponseDto;
import com.authentication.auth.dto.diary.DiaryRequestDto;
import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.domain.Diary;
import com.authentication.auth.diary.repository.DiaryRepository;
import com.authentication.auth.repository.AIResponseRepository;
import com.authentication.auth.service.diary.DiaryAnalysisService;
import com.authentication.auth.service.sse.SseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DiaryServiceImpl implements DiaryService {

  @Autowired
  private AIResponseRepository aiResponseRepository;

  @Autowired
  private DiaryRepository diaryRepository;

  @Autowired
  private DiaryAnalysisService diaryAnalysisService;

  @Autowired
  private SseService sseService;

  @Override
  public void save(String userEmail, DiaryRequestDto diaryRequestDto) {
    // 1. 사용자 조회 (이 부분은 실제 UserRepository 접근이 필요)
    // 2. Diary 엔티티 생성
    // 3. 저장
    // 4. 비동기 분석 요청
    
    // TODO: 이 메소드는 현재 사용되지 않는 것으로 보이며, 
    // 대신 DiaryManagementService.createDiaryPost 메소드가 사용되고 있습니다.
    // 만약 이 메소드가 필요하다면 적절한 구현이 필요합니다.
    throw new UnsupportedOperationException("이 메소드는 현재 구현되지 않았습니다. DiaryManagementService.createDiaryPost를 사용하세요.");
  }

  @Override
  public List<DiaryResponseDto> findDiaries(String userEmail) {
    // ...existing code...
    return null;
  }

  @Override
  public DiaryResponseDto findDiary(Long diaryId) {
    // ...existing code...
    return null;
  }

  @Override
  public String callAIServer(Long diaryId) throws IOException {
    // ...existing code...
    return "{}";
  }

  @Override
  public AIResponseDto getAnalysisByDiaryId(Long diaryId) {
    // diaryId를 사용하여 직접 AI 응답을 조회합니다.
    return aiResponseRepository.findByDiaryId(diaryId)
            .map(this::convertToDto)
            .orElse(null);
  }

  @Override
  public List<AIResponseDto> getAllAIResponses(String userId) {
    // ...existing code...
    return null;
  }

  @Override
  public Optional<AIResponseDto> getAIResponseById(String id) {
    // ...existing code...
    return null;
  }

  @Override
  public List<AIResponseDto> getAIResponsesByDateRange(String userId, String startDate, String endDate) {
    // ...existing code...
    return null;
  }

  @Override
  public AIResponseDto updateAIResponse(String id, AIResponseDto updateDto) {
    // ...existing code...
    return null;
  }

  @Override
  public void deleteAIResponse(String id) {
    // ...existing code...
  }



  @Async
  @Transactional
  @Override
  public void requestAnalysis(Long diaryId, String userId) {
    log.info("비동기 일기 분석 시작 - diaryId: {}, userId: {}", diaryId, userId);
    
    try {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("Diary not found with id: " + diaryId));

        // AI 분석 및 저장 (Diary 엔티티를 직접 전달)
        // 이 메소드 내부에서 SSE로 실시간 상태 업데이트가 전송됩니다
        AIResponseDto analysisResult = diaryAnalysisService.analyzeAndSaveDiary(diary);

        log.info("비동기 일기 분석 완료 - diaryId: {}, userId: {}", diaryId, userId);
        
    } catch (Exception e) {
        log.error("비동기 일기 분석 중 오류 발생 - diaryId: {}, userId: {}, 오류: {}", diaryId, userId, e.getMessage(), e);
        
        // 분석 실패도 SSE로 알림
        try {
            sseService.sendEventToUser(userId, "analysis_error", java.util.Map.of(
                "diaryId", diaryId,
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception sseException) {
            log.error("SSE 에러 알림 전송 실패: {}", sseException.getMessage());
        }
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
        java.util.List<AIResponseDto.EmotionDto> emotionDtos = aiResponse.getEmotions().stream()
                .map(emotion -> AIResponseDto.EmotionDto.builder()
                        .category(emotion.getCategory())
                        .intensity(emotion.getIntensity())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        builder.emotions(emotionDtos);
    }

    return builder.build();
  }
}