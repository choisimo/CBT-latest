package com.authentication.auth.controller;

import com.authentication.auth.dto.AIResponseDto;
import com.authentication.auth.dto.DiaryRequestDto;
import com.authentication.auth.service.DiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 일기 분석 (CREATE)
     */
    @PostMapping("/analyze")
    public ResponseEntity<AIResponseDto> analyzeDiary(@Valid @RequestBody DiaryRequestDto request) {
        log.info("일기 분석 요청 - 사용자: {}, 제목: {}", request.getUserId(), request.getTitle());
        AIResponseDto response = diaryService.analyzeAndSaveDiary(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 조회 (READ ALL)
     */
    @GetMapping("/responses/{userId}")
    public ResponseEntity<List<AIResponseDto>> getAllResponses(@PathVariable String userId) {
        log.info("전체 응답 조회 - 사용자: {}", userId);
        List<AIResponseDto> responses = diaryService.getAllAIResponses(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 단건 조회 (READ ONE)
     */
    @GetMapping("/response/{id}")
    public ResponseEntity<AIResponseDto> getResponse(@PathVariable String id) {
        log.info("응답 조회 - ID: {}", id);
        Optional<AIResponseDto> response = diaryService.getAIResponseById(id);
        return response.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 날짜별 조회 (READ BY DATE)
     */
    @GetMapping("/responses/{userId}/range")
    public ResponseEntity<List<AIResponseDto>> getResponsesByDate(
            @PathVariable String userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("날짜별 조회 - 사용자: {}, 기간: {} ~ {}", userId, startDate, endDate);
        List<AIResponseDto> responses = diaryService.getAIResponsesByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * 수정 (UPDATE)
     */
    @PutMapping("/response/{id}")
    public ResponseEntity<AIResponseDto> updateResponse(
            @PathVariable String id,
            @RequestBody AIResponseDto updateDto) {
        log.info("응답 수정 - ID: {}", id);
        AIResponseDto response = diaryService.updateAIResponse(id, updateDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 삭제 (DELETE)
     */
    @DeleteMapping("/response/{id}")
    public ResponseEntity<String> deleteResponse(@PathVariable String id) {
        log.info("응답 삭제 - ID: {}", id);
        diaryService.deleteAIResponse(id);
        return ResponseEntity.ok("삭제 완료");
    }

    /**
     * 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Diary API 정상 동작");
    }
}