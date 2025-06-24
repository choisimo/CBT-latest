package com.authentication.auth.service;

import com.authentication.auth.dto.AIResponseDto;
import com.authentication.auth.dto.diary.DiaryRequestDto;
import com.authentication.auth.diary.dto.DiaryResponseDto;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface DiaryService {
    void save(String userEmail, DiaryRequestDto diaryRequestDto);
    List<DiaryResponseDto> findDiaries(String userEmail);
    DiaryResponseDto findDiary(Long diaryId);
    String callAIServer(Long diaryId) throws IOException;
    AIResponseDto getAnalysisByDiaryId(Long diaryId);
    List<AIResponseDto> getAllAIResponses(String userId);
    Optional<AIResponseDto> getAIResponseById(String id);
    List<AIResponseDto> getAIResponsesByDateRange(String userId, String startDate, String endDate);
    AIResponseDto updateAIResponse(String id, AIResponseDto updateDto);
    void deleteAIResponse(String id);

    void requestAnalysis(Long diaryId, String userId);
    AIResponseDto analyzeAndSaveDiary(DiaryRequestDto request);
}
