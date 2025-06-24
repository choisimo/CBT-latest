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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
    // ...existing code...
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
    Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new RuntimeException("Diary not found with id: " + diaryId));

    String userId = diary.getUser().getLoginId();
    String diaryTitle = diary.getTitle();

    return aiResponseRepository.findByUserIdAndDiaryTitle(userId, diaryTitle)
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

  @Override
  public AIResponseDto analyzeAndSaveDiary(DiaryRequestDto request) {
    return diaryAnalysisService.analyzeAndSaveDiary(request);
  }

  @Async
  @Transactional
  @Override
  public void requestAnalysis(Long diaryId, String userId) {
    Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new RuntimeException("Diary not found with id: " + diaryId));

    DiaryRequestDto requestDto = new DiaryRequestDto();
    requestDto.setUserId(diary.getUser().getLoginId());
    requestDto.setTitle(diary.getTitle());
    requestDto.setContent(diary.getContent());
    requestDto.setDate(diary.getDate().toString());

    // AI 분석 및 저장
    AIResponseDto analysisResult = diaryAnalysisService.analyzeAndSaveDiary(requestDto);

    // 분석 완료 후 SSE로 알림 전송
    sseService.sendEventToUser(userId, "analysis_complete", analysisResult);
  }

  private AIResponseDto convertToDto(AIResponse aiResponse) {
    AIResponseDto dto = new AIResponseDto();
    // ...set properties from aiResponse to dto...
    return dto;
  }
}